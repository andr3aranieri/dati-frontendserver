package controllers.ckan

/**
  * Created by ale on 04/04/17.
  */


import javax.inject._
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.inject.{ConfigurationProvider}



@Singleton
class CkanController @Inject() (ws: WSClient, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val CKAN_URL :String = config.get.getString("app.ckan.url").get

  private val LOCAL_URL :String = config.get.getString("app.local.url").get

  private def getOrgs(orgId :String): Future[List[String]] = {
      val orgs : Future[WSResponse] = ws.url(LOCAL_URL + "/ckan/organizations/" + orgId).get()
      orgs.map( item => {
          val messages  = (item.json \ "result" \\ "message").map(_.as[String]).toList
          val datasetIds :List[String]= messages.filterNot(_.isEmpty) map { message =>
            println(message)
            val datasetId = message.split("object")(1).trim
            println(datasetId)
            datasetId
          }
           datasetIds
      })
  }

  private def getOrgDatasets(datasetIds : List[String]): Future[Seq[JsValue]] = {
    val datasetResponses: Seq[Future[JsValue]] = datasetIds.map(datasetId => {
         val response = ws.url(LOCAL_URL + "/ckan/dataset/" + datasetId).get
         println(datasetId)
         response map { x =>
           println(x.json.toString)
           (x.json \ "result").getOrElse(JsNull)
         }
    })
    val datasetFuture :Future[Seq[JsValue]] = Future.sequence(datasetResponses)
    datasetFuture
  }


  def getOrganizations = Action.async { implicit request =>
    val test = ws.url(CKAN_URL + "/api/3/action/organization_list").get
    test map { response =>
     // val bodyResponse :String = response.body
      Ok(response.json)
    }
  }


  def getOganizationRevisionList(organizationId :String) = Action.async { implicit request =>
    val url = CKAN_URL + "/api/3/action/organization_revision_list?id=" + organizationId
    val test = ws.url(url).get
    test map { response =>
      // val bodyResponse :String = response.body
      Ok(response.json)
    }
  }

  def getDataset(datasetId :String) = Action.async { implicit request =>
    val url = CKAN_URL + "/api/3/action/package_show?id=" + datasetId
    println("URL " + url)
    val test = ws.url(url).get
    test map { response =>
      // val bodyResponse :String = response.body
      Ok(response.json)
    }
  }

  def getOrganizationDataset(organizationId :String) = Action.async { implicit request =>
    for {
      datasets <- getOrgs(organizationId)
      dataset: Seq[JsValue] <- getOrgDatasets(datasets)
    } yield {
       Ok(Json.obj("result" -> Json.toJson(dataset), "success" -> JsBoolean(true)))
    }
  }

}