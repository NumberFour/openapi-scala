package com.enfore.apis.http4s

import org.http4s.Status

final case class EntityGenerator[F[_]](statusInt: Int) extends org.http4s.dsl.impl.EntityResponseGenerator[F, F] {
  override val status: Status = Status.fromInt(statusInt).right.get
}

final case class EmptyGenerator[F[_]](statusInt: Int) extends org.http4s.dsl.impl.EmptyResponseGenerator[F, F] {
  override val status: Status = Status.fromInt(statusInt).right.get
}
