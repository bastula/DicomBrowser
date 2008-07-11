package org.nrg.dcm.edit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;

public class Statement {
  private final Statement next;
  private final Constraint c;
  private final Operation op;
  
  Statement(final Operation a) {
    this(null, a);
  }
  
  Statement(final Constraint c, final Operation a) {
    this(c, a, null);
  }
  
  public Statement(final Constraint c, final Operation a, final Statement next) {
    this.op = a; this.c = c; this.next = next;
  }
  
  Statement(final Statement next, final Statement s) {
    this.c = s.c;
    this.op = s.op;
    this.next = next;
  }
  
  Statement getNext() { return next; }

  private static Collection<Action> getActions(Statement first, final File f, final DicomObject o)
  throws AttributeException {
    final List<Action> l = new LinkedList<Action>();
    for (Statement s = first; s != null; s = s.next) {
      if (s.c == null || s.c.matches(f, o))
        l.add(s.op.makeAction(o));
    }
    return l;
  }
  
  public Collection<Action> getActions(final File f, final DicomObject o)
  throws AttributeException {
    return getActions(this, f, o);
  }
  
  /**
   * For this Statement and those following, get all Operations that
   * apply to the File f, with corresponding DicomObject o.
   * In many cases (mostly, if we have no constraints) we can get by
   * without o, so o=null is valid.  If o is null, the object is loaded
   * from file if it's needed.
   * @param f the File containing the object
   * @param o the corresponding DicomObject (can be null)
   * @return List of Operations applicable to the named File
   * @throws IOException
   */
  public Collection<Operation> getOperations(final File f, DicomObject o)
  throws IOException {
    final List<Operation> ops = new LinkedList<Operation>();
    for (Statement s = this; s != null; s = s.next) {
      if (s.c == null) {
        ops.add(s.op);
      } else {
        if (o == null) {
          final DicomInputStream in = new DicomInputStream(f);
          o = in.readDicomObject();
          in.close();
        }
        if (s.c.matches(f, o))
          ops.add(s.op);
      }
    }
    return ops;
  }
  
  /**
   * @see getOperations(File f, DicomObject o)
   */
  public Collection<Operation> getOperations(final File f) throws IOException {
    return getOperations(f, null);
  }
  
  public void display() {
    System.out.print(toString());
    if (next != null) next.display();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Statement: ");
    if (c != null) { sb.append(c); sb.append(" => "); }
    sb.append(op);    
    return sb.toString();
  }
}
