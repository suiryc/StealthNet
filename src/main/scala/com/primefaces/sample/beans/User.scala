package com.primefaces.sample.beans

import java.util.Date
import scala.reflect.BeanProperty

object User {

  def apply(username: String, emailId: String, phone: String, gender: String,
      address: String) =
    new User(-1, username, emailId, phone, new Date(), gender, address)

}

class User(
  @BeanProperty
  var userId: Int = -1,
  @BeanProperty
  var username: String = null,
  @BeanProperty
  var emailId: String = null,
  @BeanProperty
  var phone: String = null,
  @BeanProperty
  var dob: Date = new Date(),
  @BeanProperty
  var gender: String = null,
  @BeanProperty
  var address: String = null
) extends Serializable
