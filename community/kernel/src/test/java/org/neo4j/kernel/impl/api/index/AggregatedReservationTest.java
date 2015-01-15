/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api.index;

import org.junit.Test;
import org.mockito.InOrder;

import org.neo4j.kernel.api.index.Reservation;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class AggregatedReservationTest
{
    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldThrowWhenTooManyAggregatesAdded()
    {
        // Given
        int size = 5;
        AggregatedReservation aggregatedReservation = new AggregatedReservation( size );

        // When
        for ( int i = 0; i < size + 1; i++ )
        {
            aggregatedReservation.add( mock( Reservation.class ) );
        }

        // Then
        // exception is thrown
    }

    @Test
    public void withdrawalShouldBeNullSafe()
    {
        // Given
        AggregatedReservation aggregatedReservation = new AggregatedReservation( 10 );

        Reservation aggregate1 = mock( Reservation.class );
        Reservation aggregate3 = mock( Reservation.class );

        aggregatedReservation.add( aggregate1 );
        aggregatedReservation.add( null );
        aggregatedReservation.add( aggregate3 );

        // When
        aggregatedReservation.withdraw();

        // Then
        InOrder order = inOrder( aggregate1, aggregate3 );
        order.verify( aggregate1 ).withdraw();
        order.verify( aggregate3 ).withdraw();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void shouldWithdrawAllAggregatedReservations()
    {
        // Given
        AggregatedReservation aggregatedReservation = new AggregatedReservation( 3 );

        Reservation aggregate1 = mock( Reservation.class );
        Reservation aggregate2 = mock( Reservation.class );
        Reservation aggregate3 = mock( Reservation.class );

        aggregatedReservation.add( aggregate1 );
        aggregatedReservation.add( aggregate2 );
        aggregatedReservation.add( aggregate3 );

        // When
        aggregatedReservation.withdraw();

        // Then
        InOrder order = inOrder( aggregate1, aggregate2, aggregate3 );
        order.verify( aggregate1 ).withdraw();
        order.verify( aggregate2 ).withdraw();
        order.verify( aggregate3 ).withdraw();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void shouldWithdrawAllAggregatedReservationsEvenIfOneOfThemThrows()
    {
        // Given
        AggregatedReservation aggregatedReservation = new AggregatedReservation( 3 );

        Reservation aggregate1 = mock( Reservation.class );
        Reservation aggregate2 = mock( Reservation.class );
        IllegalStateException exception = new IllegalStateException();
        doThrow( exception ).when( aggregate2 ).withdraw();
        Reservation aggregate3 = mock( Reservation.class );

        aggregatedReservation.add( aggregate1 );
        aggregatedReservation.add( aggregate2 );
        aggregatedReservation.add( aggregate3 );

        // When
        try
        {
            aggregatedReservation.withdraw();
            fail( "RuntimeException expected" );
        }
        catch ( RuntimeException e )
        {
            assertSame( exception, e.getCause() );
        }

        // Then
        InOrder order = inOrder( aggregate1, aggregate2, aggregate3 );
        order.verify( aggregate1 ).withdraw();
        order.verify( aggregate2 ).withdraw();
        order.verify( aggregate3 ).withdraw();
        order.verifyNoMoreInteractions();
    }
}
