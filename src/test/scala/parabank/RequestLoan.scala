package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class RequestLoan extends Simulation {

  // 1. HTTP configuration (base URL)
  val httpConf = http
    .baseUrl(url)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // 2. Scenario: Request Loan (usa query params tal como aparece en la colección)
  val scn = scenario("RequestLoanUnderLoad")
    .exec(
      http("Request a loan")
        .post("/requestLoan")
        .queryParam("customerId", customerId)
        .queryParam("amount", amount)
        .queryParam("downPayment", downPayment)
        .queryParam("fromAccountId", fromAccountId)
        .check(status.is(200))
        // Opcional: comprobar que la respuesta tenga un campo (ajusta según respuesta real)
        .check(jsonPath("$.approved").ofType[Boolean].saveAs("loanApproved"))
    )

  // 3. Load profile -> 150 usuarios concurrentes durante 60s (criterio de la historia)
  setUp(
    scn.inject(constantConcurrentUsers(150) during (60.seconds))
  ).protocols(httpConf)
    // 4. Assertions: criterios de éxito (mean response < 5000ms, success rate >=98%)
    .assertions(
      global.responseTime.mean.lt(5000),           
      global.successfulRequests.percent.gte(98)    
    )
}
