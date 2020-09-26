package kt.issues.i2004

import io.jooby.annotations.GET
import io.jooby.annotations.Path
import io.jooby.annotations.QueryParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "This is a company request")
data class CompanyRequest(
    val baseYear: Int,
    val naceCode: List<String>,
    @get:Schema(required = false, description = "blablabla", example = "HEY")
    val region: String
)

@Path("/issues/2004")
class Controller2004 {
  @Operation(
      summary = "Get all companies",
      description = "Find all companies defined by the search parameters"
  )
  @GET
  fun getAllCompanies(companyRequest: CompanyRequest): String {
    return companyRequest.toString()
  }
}
