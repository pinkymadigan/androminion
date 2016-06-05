package com.vdom.core;

import java.util.ArrayList;
import java.util.Collections;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.VictoryCard;

public class VictoryCardImpl extends CardImpl implements VictoryCard {
    public VictoryCardImpl(Cards.Kind type, int cost, int vp) {
        super(type, cost);
    }

    protected VictoryCardImpl(Builder builder) {
        super(builder);
    }

    public static class Builder extends CardImpl.Builder {
        public Builder(Cards.Kind type, int cost, int vp) {
            super(type, cost);
        }

        public VictoryCardImpl build() {
            return new VictoryCardImpl(this);
        }

    }

    @Override
    public void play(Game game, MoveContext context) {
    	play(game, context, true);
    }
    
    @Override
    public void play(Game game, MoveContext context, boolean fromHand) {
    	super.play(game, context, fromHand);
    	Player currentPlayer = context.getPlayer();
    	switch (this.getKind()) {
    	case Estate:
    		Card inheritedCard = context.player.getInheritance();
    		if (inheritedCard != null) {
    	    	// TODO
    			boolean newCard = false;
    			 if (this.numberTimesAlreadyPlayed == 0) {
		            newCard = true;
		            this.movedToNextTurnPile = false;
		            if (fromHand)
		                currentPlayer.hand.remove(this);
		           currentPlayer.playedCards.add(this);
		        }
    			
			 	GameEvent event = new GameEvent(GameEvent.EventType.PlayingCard, (MoveContext) context);
		 		event.card = this;
		 		event.newCard = newCard;
		 		game.broadcastEvent(event); 
    			
	 			context.actionsPlayedSoFar++;
		        if (context.freeActionInEffect == 0) {
		            context.actions--;
		        }
		        boolean enchantressEffect = !context.enchantressAlreadyAffected && game.enchantressAttacks(currentPlayer);
		        this.startInheritingCardAbilities(inheritedCard.getTemplateCard().instantiate());
		        // Play the inheritance virtual card
		        CardImpl cardToPlay = (CardImpl) this.behaveAsCard();
		        context.freeActionInEffect++;
		        cardToPlay.play(game, context, false);
		        context.freeActionInEffect--;

		        if (!enchantressEffect) {
			        // impersonated card stays in play until next turn?
			        if (cardToPlay.trashOnUse) {
			            int idx = currentPlayer.playedCards.lastIndexOf(this);
			            if (idx >= 0) currentPlayer.playedCards.remove(idx);
			            currentPlayer.trash(this, null, context);
			        } else if (cardToPlay.is(Type.Duration, currentPlayer) && !cardToPlay.equals(Cards.outpost)) {
			            if (!this.controlCard.movedToNextTurnPile) {
			                this.controlCard.movedToNextTurnPile = true;
			                int idx = currentPlayer.playedCards.lastIndexOf(this);
			                if (idx >= 0) {
			                    currentPlayer.playedCards.remove(idx);
			                    currentPlayer.nextTurnCards.add(this);
			                }
			            }
			        }
		        }
		        
		        event = new GameEvent(GameEvent.EventType.PlayedCard, (MoveContext) context);
		        event.card = this;
		        game.broadcastEvent(event);
		        
		        // test if any prince card left the play
		        currentPlayer.princeCardLeftThePlay(currentPlayer);
		        
		        // check for cards to call after resolving action
		        boolean isActionInPlay = isInPlay(currentPlayer);
		        ArrayList<Card> callableCards = new ArrayList<Card>();
		        Card toCall = null;
		        for (Card c : currentPlayer.tavern) {
		        	if (c.behaveAsCard().isCallableWhenActionResolved()) {
		        		if (c.behaveAsCard().doesActionStillNeedToBeInPlayToCall() && !isActionInPlay) {
		        			continue;
		        		}
		        		callableCards.add(c);
		        	}
		        }
		        if (!callableCards.isEmpty()) {
		        	Collections.sort(callableCards, new Util.CardCostComparator());
			        do {
			        	toCall = null;
			        	// we want null entry at the end for None
			        	Card[] cardsAsArray = callableCards.toArray(new Card[callableCards.size() + 1]);
			        	//ask player which card to call
			        	toCall = currentPlayer.controlPlayer.call_whenActionResolveCardToCall(context, this, cardsAsArray);
			        	if (toCall != null && callableCards.contains(toCall)) {
			        		callableCards.remove(toCall);
			        		toCall.behaveAsCard().callWhenActionResolved(context, this);
			        	}
				        // loop while we still have cards to call
				        // NOTE: we have a hack here to prevent asking for duplicate calls on an unused Royal Carriage
				        //   since technically you can ask for more and action re-played by royal carriage will ask as well
			        } while (toCall != null && toCall.equals(Cards.coinOfTheRealm) && !callableCards.isEmpty());
		        }
		 		
    		}
    		break;
    	default:
    		break;
    	}
    }

    @Override
    public CardImpl instantiate() {
        checkInstantiateOK();
        VictoryCardImpl c = new VictoryCardImpl();
        copyValues(c);
        return c;
    }

    protected void copyValues(VictoryCardImpl c) {
        super.copyValues(c);
    }

    protected VictoryCardImpl() {
    }
}
