package de.puschj.interpreter

import de.fosd.typechef.conditional._


sealed abstract class ASTNode

// =====================
// Expressions
// =====================

sealed abstract class Expression extends ASTNode
case class Num(n: Int) extends Expression
case class Id(x: String) extends Expression
case class Parens(e: Expression) extends Expression
abstract case class BinaryExpression(e1: Expression, e2: Expression) extends Expression
case class Add(override val e1: Expression, override val e2: Expression) extends BinaryExpression(e1, e2)
case class Sub(override val e1: Expression, override val e2: Expression) extends BinaryExpression(e1, e2)
case class Mul(override val e1: Expression, override val e2: Expression) extends BinaryExpression(e1, e2)
case class Div(override val e1: Expression, override val e2: Expression) extends BinaryExpression(e1, e2)

// functions
case class Call(fname: String, args: List[Opt[Expression]]) extends Expression

// classes
case class New(name: String, args: List[Opt[Expression]]) extends Expression
case class Field(expr: Expression, name: String) extends Expression
case class MethodCall(expr: Expression, call: Call) extends Expression

// =====================
// Conditions
// =====================

sealed abstract class Condition extends Expression
case class Neg(c: Condition) extends Condition
abstract case class BinaryCondition(e1: Expression, e2: Expression) extends Condition
case class Equal(override val e1: Expression, override val e2: Expression) extends BinaryCondition(e1, e2)
case class GreaterThan(override val e1: Expression, override val e2: Expression) extends BinaryCondition(e1, e2)
case class LessThan(override val e1: Expression, override val e2: Expression) extends BinaryCondition(e1, e2)
case class GreaterOE(override val e1: Expression, override val e2: Expression) extends BinaryCondition(e1, e2)
case class LessOE(override val e1: Expression, override val e2: Expression) extends BinaryCondition(e1, e2)

// =====================
// Statements
// =====================

sealed abstract class Statement extends ASTNode 
case class ExpressionStmt(expr: Expression) extends Statement
case class Assignment(expr: Expression, value: Expression) extends Statement
case class Block(stmts: List[Opt[Statement]]) extends Statement
case class While(cond: Condition, body: Block) extends Statement
case class If(cond: Condition, s1: Block, s2: Option[Block]) extends Statement
case class Assert(cond: Condition) extends Statement

// functions
case class FuncDec(name: String, args: List[String], body: Block) extends Statement
sealed abstract class FuncDef
case class FDef(args: List[String], body: Block) extends FuncDef
case class FErr(msg: String) extends FuncDef

// classes
case class ClassDec(name: String, superClass: String, fields: List[String], methods: List[Opt[FuncDec]]) extends Statement
sealed abstract class ClassDef
case class CDef(superClass: String, fields: List[String], funcStore: VAFuncStore) extends ClassDef
//case class PlainCDef(superClass: String, fields: List[String], funcStore: PlainFuncStore) extends ClassDef
case class CErr(msg: String) extends ClassDef



