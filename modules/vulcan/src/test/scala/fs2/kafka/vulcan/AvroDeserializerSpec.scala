/*
 * Copyright 2018-2023 OVO Energy Limited
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package fs2.kafka.vulcan

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.kafka.Headers
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import org.scalatest.funspec.AnyFunSpec
import vulcan.Codec

final class AvroDeserializerSpec extends AnyFunSpec {
  describe("AvroDeserializer") {
    it("can create a deserializer") {
      val deserializer =
        AvroDeserializer[Int].using(avroSettings)

      assert(deserializer.forKey.use(IO.pure).attempt.unsafeRunSync().isRight)
      assert(deserializer.forValue.use(IO.pure).attempt.unsafeRunSync().isRight)
    }

    it("raises schema errors") {
      val codec: Codec[BigDecimal] =
        Codec.decimal(-1, -1)

      val deserializer =
        avroDeserializer(codec).using(avroSettings)

      assert(deserializer.forKey.use(IO.pure).attempt.unsafeRunSync().isLeft)
      assert(deserializer.forValue.use(IO.pure).attempt.unsafeRunSync().isLeft)
    }

    it("raises IllegalArgumentException if the data is null") {
      val deserializer = AvroDeserializer[String].using(avroSettings)
      intercept[IllegalArgumentException] {
        deserializer.forKey.flatMap(_.deserialize("foo", Headers.empty, null)).unsafeRunSync()
      }
    }

    it("toString") {
      assert {
        avroDeserializer[Int].toString() startsWith "AvroDeserializer$"
      }
    }
  }

  val schemaRegistryClient: MockSchemaRegistryClient =
    new MockSchemaRegistryClient()

  val schemaRegistryClientSettings: SchemaRegistryClientSettings[IO] =
    SchemaRegistryClientSettings[IO]("baseUrl")
      .withAuth(Auth.Basic("username", "password"))
      .withMaxCacheSize(100)
      .withCreateSchemaRegistryClient { (_, _, _) =>
        IO.pure(schemaRegistryClient)
      }

  val avroSettings: AvroSettings[IO] =
    AvroSettings(schemaRegistryClientSettings)
}
