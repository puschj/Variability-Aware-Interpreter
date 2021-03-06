package de.fosd.vainterpretation.interpreter.test

import org.junit.Assert._
import org.junit._

import de.fosd.typechef.conditional.Choice
import de.fosd.typechef.conditional.Conditional
import de.fosd.typechef.conditional.ConditionalLib
import de.fosd.typechef.conditional.One
import de.fosd.typechef.featureexpr.FeatureExprFactory.True
import de.fosd.typechef.featureexpr.FeatureExpr
import de.fosd.typechef.featureexpr.FeatureExprFactory
import de.fosd.vainterpretation.interpreter.VAStore


class ConditionalArithmeticTest {
  
  val store: VAStore = new VAStore()
  val fX: FeatureExpr = FeatureExprFactory.createDefinedExternal("X")
  val fY: FeatureExpr = FeatureExprFactory.createDefinedExternal("Y")
  
  @Test
  def testAddition() {
    val x: Conditional[Int] = Choice(fX, One(1), One(2))
    val y: Conditional[Int] = Choice(fY, One(3), One(4))
    
    val f = (a: Int ,b: Int) => a+b

    assertTrue("addition failure", ConditionalLib.equals(
        Choice(fX, Choice(fY, One(4), One(5)), Choice(fY, One(5), One(6))), 
        ConditionalLib.mapCombination(x,y,f)))
    assertTrue("commutativity failure", ConditionalLib.equals(
        ConditionalLib.mapCombination(x,y,f), 
        ConditionalLib.mapCombination(y,x,f)))
  }
  
  @Test
  def testComparison() {
    val x: Conditional[Int] = Choice(fX, One(1), One(3))
    val y: Conditional[Int] = Choice(fY, One(2), One(4))
    
    val res =  ConditionalLib.mapCombination(x, y, (a: Int, b: Int) => a < b)
    
    assertTrue("comparison failure", ConditionalLib.equals(
        Choice(fX, One(true), Choice(fY, One(false), One(true))),
        res))
  }
}