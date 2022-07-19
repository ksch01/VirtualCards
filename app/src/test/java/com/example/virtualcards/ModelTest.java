package com.example.virtualcards;

import com.example.virtualcards.model.Card;
import com.example.virtualcards.model.CardStack;
import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.Model;
import com.example.virtualcards.model.interfaces.ModelSubscriber;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelTest {

    Card card1, card2, card3;
    CardStack stack1;
    Model model;
    UUID card3Id = UUID.fromString("97f038dd-0eef-4939-9774-aed90de325f8");
    UUID stack1Id = UUID.fromString("69d35246-b848-4985-96f4-f03ceb18f7b4");

    @Before
    public void setup(){
        List<GameObject> modelState = new ArrayList<>();

        card1 = new Card(0, 0, Card.Suit.CLUBS, Card.Value.ACE);
        card2 = new Card(100, 90, Card.Suit.DIAMONDS, Card.Value.EIGHT);
        card3 = new Card(card3Id, 100, 100, Card.Suit.HEARTS, Card.Value.TWO, true);
        modelState.add(card1);
        modelState.add(card2);
        modelState.add(card3);

        List<Card> cardList = new ArrayList<>();
            cardList.add(new Card(0, 100, Card.Suit.SPADES, Card.Value.ACE));
            cardList.add(new Card(50, 55, Card.Suit.SPADES, Card.Value.JACK));
        stack1 = new CardStack(stack1Id, 200, 200, cardList);
        modelState.add(stack1);

        model = TableModel.getModel();
        model.setState(modelState);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      GET GAME OBJECTS TESTS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getGameObjectsTest(){
        List<GameObject> gameObjects = model.getGameObjects();

        assertEquals(gameObjects.get(0), card1);
        assertEquals(gameObjects.get(1), card2);
        assertEquals(gameObjects.get(2), card3);
        assertEquals(gameObjects.get(3), stack1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      SUBSCRIBE VIEW TESTS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void subscribeDirectTest(){
        final boolean[] notified = new boolean[1];
        ModelSubscriber mockedSubscriber = gameObjects -> notified[0] = true;
        model.subscribeView(mockedSubscriber);

        assertTrue(notified[0]);

        model.subscribeView(null);
    }

    @Test
    public void subscribeMoveTest(){
        final boolean[] notified = new boolean[1];
        ModelSubscriber mockedSubscriber = gameObjects -> notified[0] = true;
        model.subscribeView(mockedSubscriber);
        notified[0] = false;

        model.moveObject(card1, 50, 50);
        assertTrue(notified[0]);

        model.subscribeView(null);
    }

    @Test
    public void subscribeDropTest(){
        final boolean[] notified = new boolean[1];
        ModelSubscriber mockedSubscriber = gameObjects -> notified[0] = true;
        model.subscribeView(mockedSubscriber);
        notified[0] = false;

        model.dropObject(card2, 20, 20);
        assertTrue(notified[0]);

        model.subscribeView(null);
    }

    @Test
    public void subscribeExtractWrongTest(){
        final boolean[] notified = new boolean[1];
        ModelSubscriber mockedSubscriber = gameObjects -> notified[0] = true;
        model.subscribeView(mockedSubscriber);
        notified[0] = false;

        model.extractObject(card1);
        assertFalse(notified[0]);

        model.subscribeView(null);
    }

    @Test
    public void subscribeExtractTest(){
        final boolean[] notified = new boolean[1];
        ModelSubscriber mockedSubscriber = gameObjects -> notified[0] = true;
        model.subscribeView(mockedSubscriber);
        notified[0] = false;

        model.extractObject(stack1);
        assertTrue(notified[0]);

        model.subscribeView(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      GET OBJECT TESTS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void getObjectIdTest(){
        GameObject expected = card3;
        GameObject actual = model.getObject(card3Id);

        assertEquals(expected, actual);
    }

    @Test
    public void getObjectIdWrongTest(){
        GameObject actual = model.getObject(UUID.randomUUID());

        assertEquals(null, actual);
    }

    @Test
    public void getObjectTest(){
        GameObject expected = stack1;
        GameObject actual = model.getObject(200, 200);

        assertEquals(expected, actual);
    }

    @Test
    public void getObjectOrderTest(){
        GameObject expected = card3;
        GameObject actual = model.getObject(100, 100);

        assertEquals(expected, actual);
    }

    @Test
    public void getObjectCloseTest(){
        GameObject expected = card2;
        GameObject actual = model.getObject(110, 95);

        assertEquals(expected, actual);
    }

    @Test
    public void getObjectEdgeTest(){
        GameObject expected = card3;
        GameObject actual = model.getObject(100 + Card.WIDTH - 1, 100 + Card.HEIGHT - 1);

        assertEquals(expected, actual);
    }

    @Test
    public void getObjectFailedEdgeTest(){
        GameObject actual = model.getObject(100 + Card.WIDTH, 100 + Card.HEIGHT);

        assertEquals(null, actual);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      RESERVE OBJECT TESTS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void reserveObjectTest(){
        assertTrue(model.reserveObject(card1, (byte)1));

        assertFalse(model.isAvailable(card1, (byte)0));
        assertTrue(model.isAvailable(card1, (byte)1));
        assertFalse(model.isAvailable(card1, (byte)2));
    }

    @Test
    public void reserveObjectAlreadyReservedTest(){
        assertTrue(model.reserveObject(card2, (byte)-10));
        assertFalse(model.reserveObject(card2, (byte)127));
        assertFalse(model.reserveObject(card2, (byte)-10));
        assertTrue(model.isAvailable(card2, (byte)-10));
    }

    @Test
    public void reserveNullTest(){
        assertFalse(model.reserveObject(null, (byte)11));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      MOVE OBJECT TEST
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void moveObjectValidTest(){
        model.moveObject(card1, 120, 60);

        assertEquals(120 - (Card.WIDTH * 0.5), card1.getX(), 0);
        assertEquals(60 - (Card.HEIGHT * 0.5), card1.getY(), 0);
    }

    @Test
    public void moveObjectNullTest(){
        model.moveObject(null, 10000, -1424);
    }

    @Test
    public void moveObjectOutsideTest(){
        model.moveObject(card1, 1200, -1000);

        assertEquals(TableModel.WIDTH - (Card.WIDTH), card1.getX(), 0);
        assertEquals(0, card1.getY(), 0);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      DROP OBJECT TEST
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
