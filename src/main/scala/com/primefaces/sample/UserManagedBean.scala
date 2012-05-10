package com.primefaces.sample

import javax.faces.application.FacesMessage
import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.context.FacesContext
import org.primefaces.event.{SelectEvent, UnselectEvent}
import scala.reflect.BeanProperty

@ManagedBean
@ApplicationScoped
class UserManagedBean extends Serializable {

    val userService = new UserService()

    @BeanProperty
    protected var username: String = _

    @BeanProperty
    protected var password: String = _

    @BeanProperty
    protected var searchedUser: String = _

    @BeanProperty
    protected var searchUsersResults: java.util.Collection[User] = _

    @BeanProperty
    protected var selectedUser: User = _
    
    def login(): String = {
      if("test".equalsIgnoreCase(username) && "test".equals(password))
        "home"
      else {
        val context = FacesContext.getCurrentInstance()
        context.addMessage("username", new FacesMessage("Invalid UserName and Password"))
        "login"
      }
    }

    def searchUser(): String = {
      searchUsersResults = userService.searchUsers(
        if (searchedUser == null) "" else searchedUser.trim()
      )

      "home"
    }
    
    def updateUser(): String = {
      userService.update(selectedUser)
      "home"
    }

    def onUserSelect(event: SelectEvent) =
      selectedUser = event.getObject.asInstanceOf[User]

    def onUserUnselect(event: UnselectEvent) =
      selectedUser = null

}
