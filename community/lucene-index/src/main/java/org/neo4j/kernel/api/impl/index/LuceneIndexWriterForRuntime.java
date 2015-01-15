package org.neo4j.kernel.api.impl.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

class LuceneIndexWriterForRuntime extends SimpleLuceneIndexWriter
{
    private final AtomicLong reservedDocs = new AtomicLong();

    LuceneIndexWriterForRuntime( Directory directory, IndexWriterConfig config ) throws IOException
    {
        super( directory, config );
    }

    @Override
    synchronized void reserveDocumentAdditions( int additionsCount ) throws IOException
    {
        if ( additionsCount <= 0 )
        {
            return;
        }

        if ( totalNumberOfDocumentsExceededLuceneCapacity( additionsCount ) )
        {
            // maxDoc is about to overflow, let's try to save our index

            // try to merge deletes, maybe this will fix maxDoc
            writer.forceMergeDeletes();

            if ( totalNumberOfDocumentsExceededLuceneCapacity( additionsCount ) )
            {
                // if it did not then merge everything in single segment - horribly slow, last resort
                writer.forceMerge( 1 );

                if ( totalNumberOfDocumentsExceededLuceneCapacity( additionsCount ) )
                {
                    // merging did not help - throw exception
                    throw new LuceneIndexCapacityExceededException(
                            "Unable to reserve " + additionsCount + " documents for insertion into index. " +
                            "Lucene index contains too many documents. Current limitation is " + maxDocLimit() + " " +
                            "documents per index.  Current value of maxDoc is " + writer.maxDoc() + "." );
                }
            }
        }

        // everything fine - able to reserve 'space' for new documents
        reservedDocs.addAndGet( additionsCount );
    }

    @Override
    void withdrawReservationOfDocumentAdditions( int additionsCount )
    {
        if ( additionsCount > 0 )
        {
            reservedDocs.addAndGet( -additionsCount );
        }
    }

    private boolean totalNumberOfDocumentsExceededLuceneCapacity( int newAdditions )
    {
        return (reservedDocs.get() + writer.maxDoc() + newAdditions) > maxDocLimit();
    }

    public class Reservation
    {
        final int count;

        public Reservation( int count )
        {
            this.count = count;
        }

        public void release()
        {
            reservedDocs.addAndGet( count );
        }
    }
}
