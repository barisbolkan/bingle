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

    def +(event: Event) : DeviceState = DeviceState(devices.updated(generateRecordId(event), event.device))
  }

  object DeviceState {
    def apply() : DeviceState = DeviceState(Map.empty)
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case m: AddDevice => (m.device.appName + "_" + m.device.userNo, m)
    case m: GetDevice => {
      println(m.appName)
      (m.appName + "_" + m.id, m)
    }
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case m: AddDevice => ((m.device.appName + "_" + m.device.userNo).hashCode % 10).toString
    case m: GetDevice => ((m.appName + "_" + m.id).hashCode % 10).toString
  }

  def props(settings: Settings): Props = Props(new DeviceManager(settings))

  private def generateRecordId(event: Event) : String = {
    event.device.userNo + "_" + event.device.appName
  }
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
      println("GET DEVICE GELDI")
      state(appName,id) match{

        case Left(deviceNotFound) =>
          sender() ! deviceNotFound
          println("DEVICE NOT FOUND DONDU")

        case Right(deviceInfo) =>
          sender() ! DeviceResolved(deviceInfo)
          println("DEVICE RESOLVED DONDU")

      }

    case AddDevice(deviceInfo) =>
      println("DEVICE_ADDED :" + deviceInfo.toString)
      handleEvent(DeviceAdded(deviceInfo))
  }

  private def handleEvent[E <: Event](e: => E) : Unit = {

    persistAsync(e) {
      event =>
        state += event
        context.system.eventStream.publish(event)
        //todo: 100000 i configden al
        if(lastSequenceNr != 0 && lastSequenceNr % 100 == 0)
          saveSnapshot(state)
        println("PERSIST DE ETTIM REYIZ")
    }
  }
}
