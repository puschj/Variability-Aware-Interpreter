package de.puschj.parser

import de.fosd.typechef.conditional.Opt
import de.fosd.typechef.featureexpr.FeatureExprFactory
import de.fosd.typechef.parser.java15.JavaLexer
import de.fosd.typechef.parser.java15.TokenWrapper
import de.fosd.typechef.parser.~
import de.fosd.typechef.parser.MultiFeatureParser
import de.puschj.interpreter.Add
import de.puschj.interpreter.And
import de.puschj.interpreter.Assert
import de.puschj.interpreter.Assign
import de.puschj.interpreter.Block
import de.puschj.interpreter.Bool
import de.puschj.interpreter.Call
import de.puschj.interpreter.ClassDec
import de.puschj.interpreter.Div
import de.puschj.interpreter.Eq
import de.puschj.interpreter.Expr
import de.puschj.interpreter.ExprStmt
import de.puschj.interpreter.Field
import de.puschj.interpreter.FuncDec
import de.puschj.interpreter.GoE
import de.puschj.interpreter.GrT
import de.puschj.interpreter.Var
import de.puschj.interpreter.If
import de.puschj.interpreter.LeT
import de.puschj.interpreter.LoE
import de.puschj.interpreter.MethodCall
import de.puschj.interpreter.Mul
import de.puschj.interpreter.NEq
import de.puschj.interpreter.Neg
import de.puschj.interpreter.New
import de.puschj.interpreter.Null
import de.puschj.interpreter.Num
import de.puschj.interpreter.Or
import de.puschj.interpreter.Par
import de.puschj.interpreter.Stmt
import de.puschj.interpreter.Sub
import de.puschj.interpreter.VariableProgram
import de.puschj.interpreter.While
import de.fosd.typechef.conditional.One


class WhileParser extends MultiFeatureParser() {
    type Elem = TokenWrapper
    type TypeContext = Null
    
    def identifierToken: MultiParser[Elem] = token("identifier", x=>x.getText().matches("""[a-zA-Z]([a-zA-Z0-9]|_[a-zA-Z0-9])*"""))
    
    implicit def textToken(t: String): MultiParser[Elem] =
      token(t, _.getText == t)  
      
    def intToken : MultiParser[Elem] =
      token("integer", x=>x.getText().matches("""([1-9][0-9]*)|0"""))
    
//    lazy val start: MultiParser[List[Opt[Statement]]] = "begin" ~> repOpt(declaration) ~ repOpt(statement) <~ "end" ^^ {
//      case decls~stmts => decls ++ stmts
//    }
      
    lazy val integer = intToken ^^ { x => Integer.parseInt(x.getText()) }
    
    lazy val identifier = identifierToken ^^ { x => x.getText() }
    
    lazy val nullExpr = "null" ^^ { x => Null }
    
    lazy val program = "begin" ~> (repOpt(decl | stmt)) <~ "end"
    
// =====================
// Statements
// =====================
    
    lazy val stmt: MultiParser[Stmt] = exprStmt | assign | whileStmt | block | ifStmt | assert
    
    lazy val exprStmt = expr <~ ";" ^^ {
      case expr => ExprStmt(expr)
    }
    lazy val assign = expr ~ "=" ~ (expr !) ~ ";" ^^ {
      case x~_~e~_ => new Assign(x, e)
    }
    lazy val block = "{" ~ (stmt *) ~ "}" ^^ {
      case _~stmtlst~_ => Block(stmtlst) 
    }
    lazy val whileStmt = "while" ~ "(" ~ (expr !) ~ ")" ~ block ^^ { 
      case _~_~c~_~b =>  While(c, b) 
    }
    lazy val ifStmt = "if" ~> ("(" ~> (expr !) <~ ")") ~ block ~ (("else" ~> block) ?) ^^ {
      case c~thn~els => If(c,thn,els)
    } 
    lazy val assert = "assert" ~> "(" ~> expr  <~ ")" <~ ";" ^^ {
      case c => Assert(c)
    }
    lazy val decl = classDecl | funcDecl
    
    lazy val funcDecl = "def" ~> identifier ~ ("(" ~> repSep(identifier, ",") <~ ")") ~ block ^^ {
      case funcName~funcArgs~funcBody => FuncDec(funcName, funcArgs, funcBody)
    }
    lazy val fieldDecl = "var" ~> identifier ~ (("=" ~> (expr !))?) <~ ";" ^^ {
      case name~Some(expr) => Assign(Var(name), expr)
      case name~None => Assign(Var(name), One(Null))
    }
    lazy val constDecl = "const" ~> identifier ~ ("=" ~> (expr !) <~ ";") ^^ {
      case name~expr => Assign(Var(name), expr)
    }
    
    lazy val classDecl = "class" ~> identifier ~ (("(" ~> repSep(identifier, ",") <~")")?) ~ (("extends" ~> identifier)?) ~ ("{" ~> 
                                                        repOpt(constDecl)) ~ 
                                                        repOpt(fieldDecl) ~
                                                        (repOpt(funcDecl) <~ "}") ^^ {
      case name~Some(args)~Some(superClass)~consts~fields~funcs => ClassDec(name, args, superClass, consts, fields, funcs)
      case name~Some(args)~None~consts~fields~funcs => ClassDec(name, args, "Object",  consts, fields, funcs)
      case name~None~Some(superClass)~consts~fields~funcs => ClassDec(name, List.empty[Opt[String]], superClass, consts, fields, funcs)
      case name~None~None~consts~fields~funcs => ClassDec(name, List.empty[Opt[String]], "Object",  consts, fields, funcs)
    }

// =====================
// Expressions
// =====================

    lazy val expr: MultiParser[Expr] = cond_3
    
    lazy val cond_3: MultiParser[Expr] = cond_2 ~ repPlain("&&" ~ cond_2 | "||" ~ cond_2) ^^ reduceList
    
    lazy val cond_2: MultiParser[Expr] = cond_1 ~ repPlain("==" ~ cond_1 | "!=" ~ cond_1) ^^ reduceList
    
    lazy val cond_1: MultiParser[Expr] = arith_2 ~ repPlain("<" ~ arith_2 | "<=" ~ arith_2 | ">" ~ arith_2 | ">=" ~ arith_2) ^^ reduceList
      
    lazy val arith_2: MultiParser[Expr] = arith_1 ~ repPlain("+" ~ arith_1 | "-" ~ arith_1) ^^ reduceList
    
    lazy val arith_1: MultiParser[Expr] = negation ~ repPlain("*" ~ negation | "/" ~ negation) ^^ reduceList
    
    lazy val negation: MultiParser[Expr] = (("!")?) ~ access ^^ {
      case None ~ e => e
      case _ ~ e => Neg(e)
    } 
  
    lazy val access: MultiParser[Expr] = factor ~ repPlain("." ~ (call | (identifier ^^ { case s => Var(s) })) ) ^^ reduceList
    
    lazy val factor: MultiParser[Expr] = 
      parenthesis |
      call |
      classNew |
      literal |
      nullExpr |
      integer ^^ { case i => Num(i) } |
      identifier ^^ { case i => Var(i) }
      
    lazy val literal: MultiParser[Expr] = ("false" | "true") ^^ {
      case b => Bool(b.getText.toBoolean)
    }
      
    lazy val call : MultiParser[Call] = identifier ~ ("(" ~> repSep(expr, ",") <~ ")")  ^^ {
      case name~args => Call(name, args)
    }
    
    lazy val classNew : MultiParser[Expr] = "new" ~> identifier ~ ("(" ~> repSep(expr, ",") <~ ")") ^^ {
      case classId~args => New(classId, args)
    }
      
    lazy val parenthesis : MultiParser[Par] = "(" ~> (expr) <~ ")" ^^ {
      e => Par(e)
    }
    
    val reduceList: Expr ~ List[Elem ~ Expr] => Expr = {
      case i ~ ps => (i /: ps)(reduce) 
    }
  
    def reduce(l: Expr, r: Elem ~ Expr) = r._1.getText() match {
      case "+" => Add(l, r._2)
      case "-" => Sub(l, r._2)
      case "*" => Mul(l, r._2)
      case "/" => Div(l, r._2)
      
      case "<" => LeT(l, r._2)
      case "<=" => LoE(l, r._2)
      case ">" => GrT(l, r._2)
      case ">=" => GoE(l, r._2)
      case "==" => Eq(l, r._2)
      case "!=" => NEq(l, r._2)
      case "&&" => And(l, r._2)
      case "||" => Or(l, r._2)
      
      case "." => {
        r._2 match {
          case c: Call => MethodCall(l, c)
          case Var(x) => Field(l, x)
          case e => throw new IllegalArgumentException("Bad syntax.")
        }
      }
    }

    def parse(code:String): VariableProgram = {
      val parser = new WhileParser()
      val y = parser.program(JavaLexer.lex(code), FeatureExprFactory.True)
      y match{
        case parser.Success(v,_) => {
            val p = new VariableProgram(v)
//            println(p)
            return p
        }
        case e: parser.NoSuccess =>
            throw new IllegalArgumentException("Bad syntax: "+code)
      }
    }
    
    def parseFile(filename: String): VariableProgram = {
      val source = scala.io.Source.fromFile(filename)
      val input = source.mkString
      source.close()
      return parse(input)
    }
}
