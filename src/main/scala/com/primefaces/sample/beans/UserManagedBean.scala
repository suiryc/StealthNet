package com.primefaces.sample.beans

import javax.faces.application.FacesMessage
import javax.faces.bean.{ApplicationScoped, ManagedBean}
import javax.faces.context.FacesContext
import org.primefaces.event.{SelectEvent, UnselectEvent}
import scala.reflect.BeanProperty
import com.primefaces.sample.UserService

@ManagedBean
@ApplicationScoped
class UserManagedBean extends Serializable {

    val userService = new UserService()

    @BeanProperty
    protected var searchedUser: String = _

    @BeanProperty
    protected var searchUsersResults: java.util.Collection[User] = _

    @BeanProperty
    protected var selectedUser: User = _

    def searchUser(): String = {
      searchUsersResults = userService.searchUsers(
        Option(searchedUser) map { _.trim() } getOrElse { "" }
      )

      "tutorial"
    }

    def updateUser(): String = {
      userService.update(selectedUser)
      "tutorial"
    }

    def onUserSelect(event: SelectEvent) =
      selectedUser = event.getObject.asInstanceOf[User]

    // scalastyle:off null
    def onUserUnselect(event: UnselectEvent) =
      selectedUser = null
    // scalastyle:on null

}
