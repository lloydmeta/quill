package io.getquill.context.jdbc.sqlserver

import io.getquill.{ Literal, Spec, SqlServerJdbcContext }

class JdbcContextSpec extends Spec {

  val context = testContext
  import testContext._

  "probes sqls" in {
    val p = testContext.probe("DELETE FROM TestEntity")
  }

  "run non-batched action" in {
    val insert = quote {
      qr1.insert(_.i -> lift(1))
    }
    testContext.run(insert) mustEqual 1
  }

  "provides transaction support" - {
    "success" in {
      testContext.run(qr1.delete)
      testContext.transaction {
        testContext.run(qr1.insert(_.i -> 33))
      }
      testContext.run(qr1).map(_.i) mustEqual List(33)
    }
    "failure" in {
      testContext.run(qr1.delete)
      intercept[IllegalStateException] {
        testContext.transaction {
          testContext.run(qr1.insert(_.i -> 33))
          throw new IllegalStateException
        }
      }
      testContext.run(qr1).isEmpty mustEqual true
    }
    "nested" in {
      testContext.run(qr1.delete)
      testContext.transaction {
        testContext.transaction {
          testContext.run(qr1.insert(_.i -> 33))
        }
      }
      testContext.run(qr1).map(_.i) mustEqual List(33)
    }
    "prepare" in {
      testContext.prepareParams(
        "select * from Person where name=? and age > ?", ps => (List("Sarah", 127), ps)
      ) mustEqual List("127", "'Sarah'")
    }
  }

  "Insert with returning generated with single column table" in {
    val inserted = testContext.run {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    }
    testContext.run(qr4.filter(_.i == lift(inserted))).head.i mustBe inserted
  }

  "Insert with returning generated with multiple columns and query embedded" in {
    val inserted = testContext.run {
      qr4Emb.insert(lift(TestEntity4Emb(EmbSingle(0)))).returningGenerated(_.emb.i)
    }
    testContext.run(qr4Emb.filter(_.emb.i == lift(inserted))).head.emb.i mustBe inserted
  }

  "Insert with returning with multiple columns" in {
    testContext.run(qr1.delete)
    val inserted = testContext.run {
      qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => (r.i, r.s, r.o))
    }
    (1, "foo", Some(123)) mustBe inserted
  }

  "Insert with returning with multiple columns and operations" in {
    testContext.run(qr1.delete)
    val inserted = testContext.run {
      qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => (r.i + 100, r.s, r.o.map(_ + 100)))
    }
    (1 + 100, "foo", Some(123 + 100)) mustBe inserted
  }

  "Insert with returning with multiple columns and query embedded" in {
    testContext.run(qr1Emb.delete)
    testContext.run(qr1Emb.insert(lift(TestEntityEmb(Emb("one", 1), 18L, Some(123)))))
    val inserted = testContext.run {
      qr1Emb.insert(lift(TestEntityEmb(Emb("two", 2), 18L, Some(123)))).returning(r =>
        (r.emb.i, r.o))
    }
    (2, Some(123)) mustBe inserted
  }

  "Insert with returning with multiple columns - case class" in {
    case class Return(id: Int, str: String, opt: Option[Int])
    testContext.run(qr1.delete)
    val inserted = testContext.run {
      qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123)))).returning(r => Return(r.i, r.s, r.o))
    }
    Return(1, "foo", Some(123)) mustBe inserted
  }
}

class PendingUntilFixed extends Spec {
  "remove this once sqlserver build established" in {
    new SqlServerJdbcContext(Literal, "testPostgresDB").close()
  }
}