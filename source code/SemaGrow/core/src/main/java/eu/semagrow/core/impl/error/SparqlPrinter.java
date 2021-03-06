package eu.semagrow.core.impl.error;


//import org.openrdf.query.algebra.NaryTupleOperator;
//import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Generates the SPARQL representation for a query model.
 *
 * TODO: need to extend beyond join and triple patterns.
 *
 * @author Olaf Goerlitz.
 */
public class SparqlPrinter extends QueryModelVisitorBase<RuntimeException> {

    private static final SparqlPrinter printer = new SparqlPrinter();

    private StringBuffer buffer = new StringBuffer();
    private String indent = "  ";

    /**
     * Prints the SPARQL query starting with the given query model node.
     *
     * @param root the root node of the query model to print.
     * @return the SPARQL representation of the query model.
     */
    public static String print(QueryModelNode root) {
        synchronized (printer) {
            printer.buffer.setLength(0);
            root.visit(printer);
            return printer.buffer.toString();
        }
    }

    // --------------------------------------------------------------

    @Override
//	public void meetNaryTupleOperator(NaryTupleOperator node) throws RuntimeException {
    protected void meetBinaryTupleOperator(BinaryTupleOperator node) throws RuntimeException {
        if (node instanceof Join) {
            // Sesame 3.0:
//			for (TupleExpr expr : node.getArgs()) {
//				expr.visit(this);
//			}
            // Sesame 2.3.2:
            node.getLeftArg().visit(this);
            node.getRightArg().visit(this);

        } else {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    @Override
    public void meet(Filter node) throws RuntimeException {
        // print affected expressions first
//		for (TupleExpr expr : node.getArgs()) {
//			expr.visit(this);
//		}
        node.getArg().visit(this);

        // then the applied filters conditions
        buffer.append(indent);
        buffer.append("FILTER (");
        node.getCondition().visit(this);
        buffer.append(")\n");
    }

    @Override
    public void meet(Compare node) throws RuntimeException {
        node.getLeftArg().visit(this);
        buffer.append(" ").append(node.getOperator().getSymbol()).append(" ");
        node.getRightArg().visit(this);
    }

    @Override
    public void meet(Var node) throws RuntimeException {
        if (node.hasValue()) {
            // bound variable (constant)
            Value value = node.getValue();
            if (value instanceof URI)
                buffer.append("<").append(value).append(">");
            else
                buffer.append(value);
        } else {
            // unbound variable
            if (node.isAnonymous())
                buffer.append("[]");
            else
                buffer.append("?").append(node.getName());
        }
    }

    @Override
    public void meet(ValueConstant node) throws RuntimeException {
        buffer.append(node.getValue());
    }

    @Override
    public void meet(StatementPattern node) throws RuntimeException {
        buffer.append(indent);
        for (Var var : node.getVarList()) {
            var.visit(this);
            buffer.append(" ");
        }
        buffer.append(".\n");
    }

}
