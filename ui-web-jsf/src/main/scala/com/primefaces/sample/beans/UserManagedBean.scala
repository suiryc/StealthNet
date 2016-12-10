package com.primefaces.sample.beans

import javax.faces.bean.{ApplicationScoped, ManagedBean}
import org.primefaces.event.{SelectEvent, UnselectEvent}
import scala.beans.BeanProperty
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

    def onUserSelect(event: SelectEvent): Unit =
      selectedUser = event.getObject.asInstanceOf[User]

    // scalastyle:off null
    def onUserUnselect(event: UnselectEvent): Unit =
      selectedUser = null
    // scalastyle:on null

}
