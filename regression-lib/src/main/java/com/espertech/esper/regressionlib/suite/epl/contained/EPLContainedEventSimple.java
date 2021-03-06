/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.regressionlib.suite.epl.contained;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementFormatter;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportStringBeanWithArray;
import com.espertech.esper.regressionlib.support.wordexample.SentenceEvent;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.util.CollectionUtil.buildMap;
import static com.espertech.esper.regressionlib.support.bookexample.OrderBeanFactory.*;
import static org.junit.Assert.*;

public class EPLContainedEventSimple {
    private final static String NEWLINE = System.getProperty("line.separator");

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLContainedPropertyAccess());
        execs.add(new EPLContainedNamedWindowPremptive());
        execs.add(new EPLContainedUnidirectionalJoin());
        execs.add(new EPLContainedUnidirectionalJoinCount());
        execs.add(new EPLContainedJoinCount());
        execs.add(new EPLContainedJoin());
        execs.add(new EPLContainedAloneCount());
        execs.add(new EPLContainedIRStreamArrayItem());
        execs.add(new EPLContainedSplitWords());
        execs.add(new EPLContainedArrayProperty());
        execs.add(new EPLContainedWithSubqueryResult());
        return execs;
    }

    private static class EPLContainedWithSubqueryResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create schema Person(personId string);\n" +
                    "@public @buseventtype create schema Room(roomId string);\n" +
                    "create window RoomWindow#keepall as Room;\n" +
                    "insert into RoomWindow select * from Room;\n" +
                    "insert into PersonAndRooms select personId, (select roomId from RoomWindow).selectFrom(v => new {roomId = v}) as rooms from Person;\n" +
                    "@Name('s0') select personId, roomId from PersonAndRooms[select personId, roomId from rooms@type(Room)];";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(buildMap("roomId", "r1"), "Room");
            env.sendEventMap(buildMap("roomId", "r2"), "Room");
            env.sendEventMap(buildMap("personId", "va"), "Person");
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertPropsPerRow(events, "personId,roomId".split(","), new Object[][] {{"va", "r1"}, {"va", "r2"}});

            env.undeployAll();
        }
    }

    // Assures that the events inserted into the named window are preemptive to events generated by contained-event syntax.
    // This example generates 3 contained-events: One for each book.
    // It then inserts them into a named window to determine the highest price among all.
    // The named window updates first becoming useful to subsequent events (versus last and not useful).
    private static class EPLContainedNamedWindowPremptive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "bookId".split(",");
            RegressionPath path = new RegressionPath();

            String stmtText = "@name('s0') insert into BookStream select * from OrderBean[books]";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.compileDeploy("@name('nw') create window MyWindow#lastevent as BookDesc", path);
            env.compileDeploy("insert into MyWindow select * from BookStream bs where not exists (select * from MyWindow mw where mw.price > bs.price)", path);

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"10020"}, {"10021"}, {"10022"}});
            env.listener("s0").reset();

            // higest price (27 is the last value)
            EventBean theEvent = env.iterator("nw").next();
            assertEquals(35.0, theEvent.get("price"));

            env.undeployAll();
        }
    }

    private static class EPLContainedUnidirectionalJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from " +
                "OrderBean as orderEvent unidirectional, " +
                "OrderBean[select * from books] as book, " +
                "OrderBean[select * from orderdetail.items] as item " +
                "where book.bookId=item.productId " +
                "order by book.bookId, item.amount";
            String stmtTextFormatted = "@name('s0')" + NEWLINE +
                "select *" + NEWLINE +
                "from OrderBean as orderEvent unidirectional," + NEWLINE +
                "OrderBean[select * from books] as book," + NEWLINE +
                "OrderBean[select * from orderdetail.items] as item" + NEWLINE +
                "where book.bookId=item.productId" + NEWLINE +
                "order by book.bookId, item.amount";
            env.compileDeploy(stmtText).addListener("s0");

            tryAssertionUnidirectionalJoin(env);

            env.undeployAll();

            EPStatementObjectModel model = env.eplToModel(stmtText);
            assertEquals(stmtText, model.toEPL());
            assertEquals(stmtTextFormatted, model.toEPL(new EPStatementFormatter(true)));
            env.compileDeploy(model).addListener("s0");

            tryAssertionUnidirectionalJoin(env);

            env.undeployAll();
        }

        private void tryAssertionUnidirectionalJoin(RegressionEnvironment env) {
            String[] fields = "orderEvent.orderdetail.orderId,book.bookId,book.title,item.amount".split(",");
            env.sendEventBean(makeEventOne());
            assertEquals(3, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"PO200901", "10020", "Enders Game", 10}, {"PO200901", "10020", "Enders Game", 30}, {"PO200901", "10021", "Foundation 1", 25}});
            env.listener("s0").reset();

            env.sendEventBean(makeEventTwo());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"PO200902", "10022", "Stranger in a Strange Land", 5}});
            env.listener("s0").reset();

            env.sendEventBean(makeEventThree());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"PO200903", "10021", "Foundation 1", 50}});
        }
    }

    private static class EPLContainedUnidirectionalJoinCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select count(*) from " +
                "OrderBean OrderBean unidirectional, " +
                "OrderBean[books] as book, " +
                "OrderBean[orderdetail.items] item " +
                "where book.bookId = item.productId order by book.bookId asc, item.amount asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{3L});

            env.sendEventBean(makeEventTwo());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{1L});

            env.sendEventBean(makeEventThree());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{1L});

            env.sendEventBean(makeEventFour());
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLContainedJoinCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "count(*)".split(",");
            String stmtText = "@name('s0') select count(*) from " +
                "OrderBean[books]#unique(bookId) book, " +
                "OrderBean[orderdetail.items]#keepall item " +
                "where book.bookId = item.productId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{3L}});

            env.sendEventBean(makeEventTwo());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{4L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{4L}});

            env.sendEventBean(makeEventThree());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{5L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{5L}});

            env.sendEventBean(makeEventFour());
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "count(*)".split(","), new Object[]{8L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{8L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "book.bookId,item.itemId,amount".split(",");
            String stmtText = "@name('s0') select book.bookId,item.itemId,amount from " +
                "OrderBean[books]#firstunique(bookId) book, " +
                "OrderBean[orderdetail.items]#keepall item " +
                "where book.bookId = item.productId " +
                "order by book.bookId, item.itemId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"10020", "A001", 10}, {"10020", "A003", 30}, {"10021", "A002", 25}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"10020", "A001", 10}, {"10020", "A003", 30}, {"10021", "A002", 25}});
            env.listener("s0").reset();

            env.sendEventBean(makeEventTwo());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"10022", "B001", 5}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"10020", "A001", 10}, {"10020", "A003", 30}, {"10021", "A002", 25}, {"10022", "B001", 5}});
            env.listener("s0").reset();

            env.sendEventBean(makeEventThree());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"10021", "C001", 50}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"10020", "A001", 10}, {"10020", "A003", 30}, {"10021", "A002", 25}, {"10021", "C001", 50}, {"10022", "B001", 5}});
            env.listener("s0").reset();

            env.sendEventBean(makeEventFour());
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLContainedAloneCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "count(*)".split(",");

            String stmtText = "@name('s0') select count(*) from OrderBean[books]";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{3L}});

            env.sendEventBean(makeEventFour());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5L});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{5L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedPropertyAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') @IterableUnbound select bookId from OrderBean[books]").addListener("s0");
            env.compileDeploy("@name('s1') @IterableUnbound select books[0].author as val from OrderBean(books[0].bookId = '10020')");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "bookId".split(","), new Object[][]{{"10020"}, {"10021"}, {"10022"}});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), "bookId".split(","), new Object[][]{{"10020"}, {"10021"}, {"10022"}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s1"), "val".split(","), new Object[][]{{"Orson Scott Card"}});

            env.sendEventBean(makeEventFour());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "bookId".split(","), new Object[][]{{"10031"}, {"10032"}});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), "bookId".split(","), new Object[][]{{"10031"}, {"10032"}});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s1"), "val".split(","), new Object[][]{{"Orson Scott Card"}});

            // add where clause
            env.undeployAll();
            env.compileDeploy("@name('s0') select bookId from OrderBean[books where author='Orson Scott Card']").addListener("s0");
            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "bookId".split(","), new Object[][]{{"10020"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLContainedIRStreamArrayItem implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') @IterableUnbound select irstream bookId from OrderBean[books[0]]";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "bookId".split(","), new Object[][]{{"10020"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), "bookId".split(","), new Object[][]{{"10020"}});

            env.sendEventBean(makeEventFour());
            assertNull(env.listener("s0").getLastOldData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), "bookId".split(","), new Object[][]{{"10031"}});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), "bookId".split(","), new Object[][]{{"10031"}});

            env.undeployAll();
        }
    }

    private static class EPLContainedSplitWords implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') insert into WordStream select * from SentenceEvent[words]";

            String[] fields = "word".split(",");
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SentenceEvent("I am testing this"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"I"}, {"am"}, {"testing"}, {"this"}});

            env.undeployAll();
        }
    }

    private static class EPLContainedArrayProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create objectarray schema ContainedId(id string)", path);
            env.compileDeploy("@name('s0') select * from SupportStringBeanWithArray[select topId, * from containedIds @type(ContainedId)]", path).addListener("s0");
            env.sendEventBean(new SupportStringBeanWithArray("A", "one,two,three".split(",")));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "topId,id".split(","),
                new Object[][]{{"A", "one"}, {"A", "two"}, {"A", "three"}});
            env.undeployAll();
        }
    }

}
