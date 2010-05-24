//
// Generated by JTB 1.3.2
//

package jtb.syntaxtree;

/**
 * Grammar production:
 * f0 -> "import"
 * f1 -> [ "static" ]
 * f2 -> Name()
 * f3 -> [ "." "*" ]
 * f4 -> ";"
 */
public class ImportDeclaration implements Node {
   static final long serialVersionUID = 20050923L;

   private Node parent;
   public NodeToken f0;
   public NodeOptional f1;
   public Name f2;
   public NodeOptional f3;
   public NodeToken f4;

   public ImportDeclaration(NodeToken n0, NodeOptional n1, Name n2, NodeOptional n3, NodeToken n4) {
      f0 = n0;
      if ( f0 != null ) f0.setParent(this);
      f1 = n1;
      if ( f1 != null ) f1.setParent(this);
      f2 = n2;
      if ( f2 != null ) f2.setParent(this);
      f3 = n3;
      if ( f3 != null ) f3.setParent(this);
      f4 = n4;
      if ( f4 != null ) f4.setParent(this);
   }

   public ImportDeclaration(NodeOptional n0, Name n1, NodeOptional n2) {
      f0 = new NodeToken("import");
      if ( f0 != null ) f0.setParent(this);
      f1 = n0;
      if ( f1 != null ) f1.setParent(this);
      f2 = n1;
      if ( f2 != null ) f2.setParent(this);
      f3 = n2;
      if ( f3 != null ) f3.setParent(this);
      f4 = new NodeToken(";");
      if ( f4 != null ) f4.setParent(this);
   }

   public void accept(jtb.visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(jtb.visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
   public <R> R accept(jtb.visitor.GJNoArguVisitor<R> v) {
      return v.visit(this);
   }
   public <A> void accept(jtb.visitor.GJVoidVisitor<A> v, A argu) {
      v.visit(this,argu);
   }
   public void setParent(Node n) { parent = n; }
   public Node getParent()       { return parent; }
}
