package stealthnet.scala.ui.web.beans

import javax.faces.bean.{ManagedBean, SessionScoped}
import scala.beans.BeanProperty

@ManagedBean
@SessionScoped
class UserSession extends Serializable {

  @BeanProperty
  protected var logged: Boolean = false

}
