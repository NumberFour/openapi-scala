package com.enfore.apis.http4s

import cats.arrow.FunctionK
import org.http4s.Status

final case class EntityGenerator[F[_]](statusInt: Int) extends org.http4s.dsl.impl.EntityResponseGenerator[F, F] {
  override val status: Status = Status.fromInt(statusInt).getOrElse(throw new Exception("Unexpected Status generated"))

  override def liftG: FunctionK[F, F] = new FunctionK[F, F] {
    override def apply[A](fa: F[A]): F[A] = fa
  }
}

final case class EmptyGenerator[F[_]](statusInt: Int) extends org.http4s.dsl.impl.EmptyResponseGenerator[F, F] {
  override val status: Status = Status.fromInt(statusInt).getOrElse(throw new Exception("Unexpected Status generated"))
}
