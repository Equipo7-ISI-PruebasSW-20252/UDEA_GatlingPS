package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class LoginTest extends Simulation{

  // 1 Http Conf
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    //Verificar de forma general para todas las solicitudes
    .check(status.is(200))

  // 2 Scenario Definition
  val scn = scenario("Login").
    exec(http("login")
      .get(s"/login/$username/$password")
       //Recibir información de la cuenta
      .check(status.is(200))
    ).pause(1) // breve pausa para no bombardear instantáneamente

  // 3 Load Scenario
  // 3 - Injection (closed model): ramp-up suave, luego steady-state a 100 concurrentes
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 100 during (30.seconds), // warm-up / ramp
    constantConcurrentUsers(100) during (3.minutes)    // steady state para medir p95
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    // Assertions enfocadas en el criterio CA01
    details("login").responseTime.percentile(95).lte(2000), // p95 <= 2000 ms
    //global.successfulRequests.percent.gte(5)               // al menos 5% exitosas
  )
}
