package com.primefaces.sample

import javax.faces.bean.{ManagedBean, SessionScoped}
import scala.reflect.BeanProperty

@ManagedBean
@SessionScoped
class UserSession extends Serializable {

  @BeanProperty
  protected var logged: Boolean = false

}
