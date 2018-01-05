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

        // URL vs URI -- URI much better for performance
        final Map<URI, String> bodies =Urls.all()
                .subscribeOn(Schedulers.io())
                .flatMap(url->
                        UrlDownloader.download(url)
                                .map( s-> Pair.of(url,s))
                                .subscribeOn(Schedulers.io())
                ).toMap( p-> p.getLeft().toURI(), p->p.getRight())
                .blockingGet();

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
