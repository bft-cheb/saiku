package org.saiku.query;

import junit.framework.TestCase;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapStatement;
import org.olap4j.OlapWrapper;
import org.olap4j.Position;
import org.olap4j.metadata.Member;

import java.util.List;

public class OlapTest extends TestCase {

	private TestContext context = TestContext.instance();


	public void testConnection() {
		OlapConnection con = context.createConnection();
		try {
			assertEquals(1, con.getOlapCatalogs().size());
			assertEquals("FoodMart", con.getOlapCatalogs().get(0).getName());
		} catch (OlapException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testVisualTotalsCaptionBug() {

		try {
			OlapConnection connection = context.createConnection();
			final OlapWrapper wrapper = connection;
			OlapConnection olapConnection = (OlapConnection) wrapper.unwrap(OlapConnection.class);
			OlapStatement stmt = olapConnection.createStatement();
			CellSet cellSet = stmt.executeOlapQuery(
	                "select {[Measures].[Unit Sales]} on columns, "
	                        + "{VisualTotals("
	                        + "    {[Product].[Food].[Baked Goods].[Bread],"
	                        + "     [Product].[Food].[Baked Goods].[Bread].[Bagels],"
	                        + "     [Product].[Food].[Baked Goods].[Bread].[Muffins]},"
	                        + "     \"**Subtotal - *\")} on rows "
	                        + "from [Sales]");
			
	        List<Position> positions = cellSet.getAxes().get(1).getPositions();
	        Member member = positions.get(0).getMembers().get(0);
	        assertEquals("*Subtotal - Bread", member.getName());
	        assertEquals("*Subtotal - Bread", member.getCaption());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testVisualTotalsHierarchizeBug() {

		try {
			OlapConnection connection = context.createConnection();
			final OlapWrapper wrapper = connection;
			OlapConnection olapConnection = (OlapConnection) wrapper.unwrap(OlapConnection.class);
			OlapStatement stmt = olapConnection.createStatement();
			CellSet cellSet = stmt.executeOlapQuery(
	                "select {[Measures].[Unit Sales]} on columns, "
	                        + "VisualTotals(Hierarchize("
	                        + "    {[Product].[Product Family].Members,"
	                        + "    [Product].[Drink].[Beverages],"
	                        + "    [Product].[Drink].[Dairy]}"
	                        + ", PRE)"
	                        + "     ,\"**Subtotal - *\")"
	                        + "                           on rows "
	                        + "from [Sales]");
			
			String s = TestUtil.toString(cellSet);
			TestUtil.assertEqualsVerbose(
					"Axis #0:\n"
			                + "{}\n"
			                + "Axis #1:\n"
			                + "{[Measures].[Unit Sales]}\n"
			                + "Axis #2:\n"
			                + "{[Product].[*Subtotal - Drink]}\n"
			                + "{[Product].[Drink].[Beverages]}\n"
			                + "{[Product].[Drink].[Dairy]}\n"
			                + "{[Product].[Food]}\n"
			                + "{[Product].[Non-Consumable]}\n"
			                + "Row #0: 17,759\n"
			                + "Row #1: 13,573\n"
			                + "Row #2: 4,186\n"
			                + "Row #3: 191,940\n"
			                + "Row #4: 50,236\n"
			          ,s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
