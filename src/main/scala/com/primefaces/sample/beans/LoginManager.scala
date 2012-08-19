package com.primefaces.sample.beans

import javax.faces.application.FacesMessage
import javax.faces.bean.{ManagedBean, ManagedProperty, RequestScoped}
import javax.faces.context.FacesContext
import javax.servlet.http.HttpSession
import scala.reflect.BeanProperty

@ManagedBean
@RequestScoped
class LoginManager {

  @BeanProperty
  protected var reason: String = _

  @BeanProperty
  protected var username: String = _

  @BeanProperty
  protected var password: String = _

  @ManagedProperty(value="#{userSession}")
  @BeanProperty
  protected var userSession: UserSession = _

  def login(): String = {
    if ((username == "test") && (password == "test")) {
      userSession.setLogged(true)
      "home?faces-redirect=true"
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

  def handleReason() =
    Option(reason) foreach {
      _.toLowerCase match {
        case "logout" =>
          logout()

        case _ =>
      }
    }

  private def logout() {
    val context = FacesContext.getCurrentInstance.getExternalContext

    context.getSession(false).asInstanceOf[HttpSession].invalidate()
    context.redirect(context.getRequestContextPath)
  }

}
