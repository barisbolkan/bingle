package com.aisle_n.bingle.actors

import java.util.Date

import akka.actor.Props
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.aisle_n.bingle.configuration.Settings

/**
  * Created by alimerttaskin on 01.03.2018.
  */
final case class DeviceInfo(userNo: String,
                            deviceToken: String,
                            deviceOS: String,
                            pushEnabled: Boolean,
                            lastUpdateDate: Date,
                            deleted: Boolean,
                            appName: String)

object DeviceManager {

  sealed trait Command

  final case class GetDevice(appName: String , id: String) extends Command
  final case class AddDevice(device: DeviceInfo) extends Command

  sealed trait Event {
    val device : DeviceInfo
  }
  final case class DeviceAdded(device: DeviceInfo) extends Event
  final case class DeviceResolved(device: DeviceInfo) extends Event

  final case class DeviceNotFound(appName: String, customerNo: String)

  type MaybeDevice[+A] = Either[DeviceNotFound, A]

  final case class DeviceState(devices : Map[String, DeviceInfo]){

    def apply(appName: String, userNo: String): MaybeDevice[DeviceInfo] =
      devices.get(appName + "_" + userNo).toRight(DeviceNotFound(appName, userNo))

    def +(event: Event) : DeviceState = DeviceState(devices.updated(event.device.appName+"_"+event.device.userNo, event.device))

    def deviceTokenUpdated (appName: String, userNo: String, deviceToken: String) : Boolean = {
      devices.get(appName + "_" + userNo) match {
        case Some(deviceInfo) => if(deviceInfo.deviceToken == deviceToken) false else true
        case None => true
      }
    }
  }

  object DeviceState {
    def apply() : DeviceState = DeviceState(Map.empty)
  }

  def extractEntityId: ShardRegion.ExtractEntityId = {
    case m: AddDevice => (m.device.appName + "_" + m.device.userNo, m)
    case m: GetDevice => (m.appName + "_" + m.id, m)
  }

  def extractShardId: ShardRegion.ExtractShardId = {
    case m: AddDevice => ((m.device.appName + "_" + m.device.userNo).hashCode % 10).toString
    case m: GetDevice => ((m.appName + "_" + m.id).hashCode % 10).toString
  }

  def props(settings: Settings): Props = Props(new DeviceManager(settings))

}

class DeviceManager(settings: Settings) extends PersistentActor {

  import com.aisle_n.bingle.actors.DeviceManager._

  private var state = DeviceState()

  override def persistenceId: String = self.path.name

  override def receiveRecover : Receive = {
    //Sadece deviceAdded persist edildigi icin sorun yok
    case event: Event =>
      state += event
    case SnapshotOffer(_, snapshot: DeviceState) =>
      state = snapshot
    case RecoveryCompleted =>
      println(RecoveryCompleted)
    //TODO: DO STH
  }

  override def receiveCommand : Receive = {
    //todo: appName implementation
    case GetDevice(appName,id) =>
      state(appName, id) match {
        case Left(deviceNotFound) =>
          sender() ! deviceNotFound
        case Right(deviceInfo) =>
          sender() ! DeviceResolved(deviceInfo)
      }
    case AddDevice(deviceInfo) =>
      handleEvent(DeviceAdded(deviceInfo))
  }

  private def handleEvent[E <: Event](e: => E) : Unit = {

    if(state.deviceTokenUpdated(e.device.appName, e.device.userNo, e.device.deviceToken)){
      persistAsync(e) {
        event =>
          state += event
          context.system.eventStream.publish(event)
          if(lastSequenceNr != 0 && lastSequenceNr % settings.snapshotCount == 0)
            saveSnapshot(state)
      }
    }
  }
}