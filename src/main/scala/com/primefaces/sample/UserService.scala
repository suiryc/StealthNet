package com.primefaces.sample

import java.util.Date
import scala.collection.mutable
import scala.collection.JavaConversions._

object UserService {

  private var USERS_TABLE = mutable.HashMap[Int, User]()
  private var maxUserId = 1

  def create(user: User): Int = {
    assert(user != null)

    val userId = maxUserId
    user.userId = userId
    USERS_TABLE += userId -> user
    maxUserId += 1

    userId
  }

  def delete(user: User) {
    assert(user != null)

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
  def create(user: User) = UserService.create(user)

  /* XXX - not thread-safe */
  def delete(user: User) = UserService.delete(user)

  def getAllUsers = USERS_TABLE.values.toList:java.util.List[User]

  def getUser(userId: Int): User = USERS_TABLE.get(userId) match {
    case Some(user) => user
    case None => null
  }

  def searchUsers(username: String) = {
    var searchResults = mutable.ArrayBuffer[User]()
    for (user <- USERS_TABLE.values) {
      if((user.username != null) &&
          user.username.toLowerCase().trim().startsWith(
            if (username == null) "" else username.toLowerCase().trim()
          )
        )
        searchResults += user
    }
    /* We need to return a Java Collection */
    searchResults:java.util.List[User]
  }

  def update(user: User) {
    assert((user != null) && (USERS_TABLE contains user.userId))
    USERS_TABLE(user.userId) = user
  }

}
