package com.nurkiewicz.rxjava;

import com.nurkiewicz.rxjava.util.UrlDownloader;
import com.nurkiewicz.rxjava.util.Urls;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class R21_FlatMap {

    /**
     * Hint: UrlDownloader.download()
     * Hint: flatMap(), maybe concatMap()?
     * Hint: toList()
     * Hint: blockingGet()
     *
     * don't want to get Flowable<Flowable<String>> ...
     * Flowable<Flowable<T>>
     * Optional<Optional<T>>
     *     ...
     * want to use flatMap ... subscribes to inner stream and pushes to the outer stream
     *
     * Hint: Flowable.toMap(), point here is that need to get parallelism on the flat map
     * for it do do anything
     *
     * NOTE there is a default limit on concurrency in flatMap, it is 128
     */
    @Test
    public void shouldDownloadAllUrlsInArbitraryOrder() throws Exception {
        Flowable<URL> urls = Urls.all();

        //when
        List<String> bodies = Urls.all()
                .subscribeOn(Schedulers.io())
                .flatMap(url-> UrlDownloader.download(url).subscribeOn(Schedulers.io()))
                .toList()
                .blockingGet();

        //then
        assertThat(bodies).hasSize(996);
        assertThat(bodies).contains("<html>www.twitter.com</html>", "<html>www.aol.com</html>", "<html>www.mozilla.org</html>");
    }

    /**
     * Hint: Pair.of(...)
     * Hint: Flowable.toMap(), point here is that need to get parallelism on the flat map
     * for it do do anything
     */
    @Test
    public void shouldDownloadAllUrls() throws Exception {
        //given
        Flowable<URL> urls = Urls.all();

        //when
        //WARNING: URL key in HashMap is a bad idea here
//		Map<URI, String> bodies = null; //urls...
        // Single is a Flowable that has one item in it

        // URL vs URI -- URI much better for performance, url does a dns access I think
        // to compare two url you make a dns query, if network goes down, comparison
        // is by string ... this means that url can be equal with dns access and not
        // when a string comparison is done, note that a Single is a Flowable with one element
        final Map<URI, String> bodies =Urls.all()
                .subscribeOn(Schedulers.io())
                .flatMap(url->  // this flatMap returns a Flowable<String>  or it does it without the map
                        UrlDownloader.download(url)
                                .map( s-> Pair.of(url,s))
                                .subscribeOn(Schedulers.io()), 1000  // increasing the concurrency here -- default is 128
                ).toMap( p-> p.getLeft().toURI(), p->p.getRight())
                .blockingGet();  // works on a Single

        //then
        assertThat(bodies).hasSize(996);
        assertThat(bodies).containsEntry(new URI("http://www.twitter.com"), "<html>www.twitter.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.aol.com"), "<html>www.aol.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.mozilla.org"), "<html>www.mozilla.org</html>");
    }

    /**
     * Hint: flatMap with int parameter
     */
    @Test
    public void downloadThrottled() throws Exception {
        //given
        Flowable<URL> urls = Urls.all().take(20);

        //when
        //Use UrlDownloader.downloadThrottled()
//        Map<URI, String> bodies = new HashMap<>();

        final Map<URI, String> bodies =Urls.all()
                .subscribeOn(Schedulers.io())
                .flatMap(url->
                        UrlDownloader.downloadThrottled(url)
                                .map( s-> Pair.of(url,s))
                                .subscribeOn(Schedulers.io()),10
                ).toMap( p-> p.getLeft().toURI(), p->p.getRight())
                .blockingGet();

        //then
        assertThat(bodies).containsEntry(new URI("http://www.twitter.com"), "<html>www.twitter.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.adobe.com"), "<html>www.adobe.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.bing.com"), "<html>www.bing.com</html>");
    }



    private URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}

/*

 flatMap         -- creates all threads and subscribe to all at the same time
 concatMap       -- is same as flatMap with maxConcurrencey 1, concatMap preserves order, no concurrency
 concatMapEager  -- allows concurrency but still preserves order, will wait as necessary for threads to complete before
                 -- releasing the result,  disadvantage is that a slow thread can block others also holds onto resource

 */