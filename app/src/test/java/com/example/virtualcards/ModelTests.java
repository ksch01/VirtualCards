package com.example.virtualcards;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

import com.example.virtualcards.model.Card;
import com.example.virtualcards.model.CardStack;
import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;

public class ModelTests {
    Card firstExampleCard;
    CardStack firstExampleCardStack;
    GameObject firstExampleGameObject;
    TableModel firstExampleModel;

    @Test
    public void cardConstructorTest() {
        firstExampleCard = new Card(0, 0, Card.Suit.DIAMONDS, Card.Value.TWO);

        assertEquals(firstExampleCard.getSuit(), Card.Suit.DIAMONDS);
        assertEquals(firstExampleCard.getValue(), Card.Value.TWO);
        assertFalse(firstExampleCard.isFaceUp());
        assertEquals(firstExampleCard.getX(), 0,0);
        assertEquals(firstExampleCard.getY(), 0,0);
    }

/*    @Test
    public void cardStackConstructorTest() {
        firstExampleCardStack = new CardStack(0,0, Card.Suit.DIAMONDS, Card.Value.TWO);

        assertEquals(firstExampleCardStack.getSuit(), Card.Suit.DIAMONDS);
        assertEquals(firstExampleCardStack.getValue(), Card.Value.TWO);
        assertFalse(firstExampleCardStack.isFaceUp());
        assertEquals(firstExampleCardStack.getX(), 0,0);
        assertEquals(firstExampleCardStack.getY(), 0,0);
    }*/

    @Test
    public void modelInitializeTest() {
        firstExampleModel = TableModel.getModel();
        float centerX = (TableModel.WIDTH - Card.WIDTH) * 0.5f, centerY = (TableModel.HEIGHT - Card.HEIGHT) * 0.5f;

        GameObject nullObject = firstExampleModel.getObject(1, 1);
        assertNull(nullObject);

        CardStack getObjectCardStack =
                (CardStack) firstExampleModel.getObject(centerX + 1, centerY + 1);
        assertEquals(Card.Value.ACE, getObjectCardStack.getValue());
        assertEquals(Card.Suit.DIAMONDS, getObjectCardStack.getSuit());

        firstExampleModel.moveObject(getObjectCardStack,10,10);
        assertEquals(Card.Value.ACE,
                ((CardStack) firstExampleModel.getObject(11, 11)).getValue());
        assertNull(firstExampleModel.getObject(centerX + 1, centerY + 1));

        assertFalse(getObjectCardStack.isFaceUp());
        firstExampleModel.hitObject(getObjectCardStack);
        assertTrue(getObjectCardStack.isFaceUp());
        firstExampleModel.hitObject(getObjectCardStack);
        assertFalse(getObjectCardStack.isFaceUp());

        Card extractCard1 = (Card) firstExampleModel.extractObject(getObjectCardStack);
        firstExampleModel.moveObject(extractCard1,100,100);
        assertNotEquals(extractCard1.getValue(),getObjectCardStack.getValue());
        Card extractCard2 = (Card) firstExampleModel.extractObject(getObjectCardStack);
        firstExampleModel.moveObject(extractCard2,100,100);
        assertNotEquals(extractCard2.getValue(),getObjectCardStack.getValue());
        assertNotEquals(extractCard1, extractCard2);

        GameObject extractNull = firstExampleModel.extractObject(extractCard1);
        assertNull(extractNull);
    }
/*
    @Test
    public void cardStackTest() {
        firstExampleCardStack = new CardStack(0,0, Card.Suit.DIAMONDS, Card.Value.TWO);

        assertFalse(firstExampleCardStack.isFaceUp());

        Card.Suit firstSuit = firstExampleCardStack.getSuit();
        Card.Value firstValue = firstExampleCardStack.getValue();
        Card firstCard = firstExampleCardStack.popCard();
        Card.Suit secondSuit = firstExampleCardStack.getSuit();
        Card.Value secondValue = firstExampleCardStack.getValue();
        Card secondCard = firstExampleCardStack.popCard();

        assertNotEquals(firstSuit, secondSuit);
        assertNotEquals(firstValue, secondValue);
        assertNotEquals(firstCard, secondCard);
        assertNotEquals(firstCard, firstExampleCardStack.popCard());
        assertNotEquals(secondCard, firstExampleCardStack.popCard());

        assertFalse(firstExampleCardStack.isFaceUp());
    }*/
}
