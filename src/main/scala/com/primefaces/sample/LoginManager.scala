package com.primefaces.sample

import javax.faces.application.FacesMessage
import javax.faces.bean.{ManagedBean, ManagedProperty, RequestScoped}
import javax.faces.context.FacesContext
import javax.servlet.http.HttpSession
import scala.reflect.BeanProperty

@ManagedBean
@RequestScoped
class LoginManager {

  @BeanProperty
  protected var username: String = _

  @BeanProperty
  protected var password: String = _

  @ManagedProperty(value="#{userSession}")
  @BeanProperty
  protected var userSession: UserSession = _

  def login(): String = {
    if ((username == "test") && (password == "test")) {
      /* XXX - invalidate session and reset userSession ? */
      userSession.setLogged(true)
      "home"
    }
    else {
      FacesContext.getCurrentInstance.addMessage("loginManager",
        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Authentication failed",
          "Invalid username or password"
        )
      )
      "login"
    }
  }

  def logout(): String = {
    invalidateSession()
    "login"
  }

  private def invalidateSession() = FacesContext.getCurrentInstance.
    getExternalContext.getSession(false).asInstanceOf[HttpSession].invalidate()

}
