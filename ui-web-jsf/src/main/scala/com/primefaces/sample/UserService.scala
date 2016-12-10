package com.primefaces.sample

import com.primefaces.sample.beans.User
import java.util
import scala.collection.mutable
import scala.collection.JavaConverters._

object UserService {

  private val USERS_TABLE = mutable.HashMap[Int, User]()
  private var maxUserId = 1

  def create(user: User): Int = {
    // scalastyle:off null
    assert(user != null)
    // scalastyle:on null

    val userId = maxUserId
    user.userId = userId
    USERS_TABLE += userId -> user
    maxUserId += 1

    userId
  }

  def delete(user: User) {
    // scalastyle:off null
    assert(user != null)
    // scalastyle:on null

    USERS_TABLE -= user.userId
    if (maxUserId == user.userId)
      maxUserId = USERS_TABLE.keys.foldLeft(1) {
        (maxId, id) => if (id > maxId) id else maxId
      }
  }

  create(User("Administrator", "admin@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Guest", "guest@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("John", "John@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Paul", "Paul@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("raju", "raju@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("raghav", "raghav@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("caren", "caren@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Mike", "Mike@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("Steve", "Steve@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Polhman", "Polhman@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("Rogermoor", "Rogermoor@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Robinhood", "Robinhood@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("Sean", "Sean@gmail.com", "9000510456", "M", "Hyderabad"))
  create(User("Gabriel", "Gabriel@gmail.com", "9247469543", "M", "Hyderabad"))
  create(User("raman", "raman@gmail.com", "9000510456", "M", "Hyderabad"))

}

class UserService {

  import UserService._

  /* XXX - not thread-safe */
  def create(user: User): Int = UserService.create(user)

  /* XXX - not thread-safe */
  def delete(user: User): Unit = UserService.delete(user)

  def getAllUsers: util.List[User] = USERS_TABLE.values.toList.asJava:java.util.List[User]

  def getUser(userId: Int): User = USERS_TABLE.get(userId).orNull

  def searchUsers(username: String): util.List[User] =
    /* We need to return a Java Collection */
    USERS_TABLE.values.toList.filter { user =>
      Option(user.username) map { v =>
        v.toLowerCase.trim().startsWith(
          Option(username) map { _.toLowerCase.trim() } getOrElse { "" }
        )
      } getOrElse {
        false
      }
    }.asJava:java.util.List[User]

  def update(user: User) {
    // scalastyle:off null
    assert((user != null) && (USERS_TABLE contains user.userId))
    // scalastyle:on null
    USERS_TABLE(user.userId) = user
  }

}
