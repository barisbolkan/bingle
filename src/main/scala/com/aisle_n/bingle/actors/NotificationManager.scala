package com.aisle_n.bingle.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.aisle_n.bingle.actors.DeviceManager._
import com.aisle_n.bingle.configuration.Settings

case class Notify(appName: String, customerNo: String)
case class NotificationCreated(id: String)

class NotificationManager(deviceManager: ActorRef, msg: String)(settings: Settings)
  extends Actor with ActorLogging {

  var caller: ActorRef = null

  override def receive: Receive = {
    case Notify(appName, customerNo) =>
      log.debug("Sending notification [App:" + appName +"][CustomerNo: " + customerNo + "][Message: " + msg + "]")
      caller = sender()
      deviceManager ! GetDevice(appName, customerNo)
    case DeviceResolved(deviceInfo) =>
      log.debug("[DeviceId: " + deviceInfo.deviceToken + "][DeviceType: " + deviceInfo.deviceOS + "]")
      caller ! NotificationCreated(UUID.randomUUID().toString)
    // TODO: Send notification to GCM|FCM
    case DeviceNotFound(appName,customerNo) =>
      log.debug("Device Not Found: " + appName + " " + "Customer No: " + customerNo)
  }
}
