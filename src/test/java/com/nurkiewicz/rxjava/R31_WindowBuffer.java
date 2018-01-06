package com.nurkiewicz.rxjava;

import io.reactivex.Flowable;
import org.junit.Ignore;
import org.junit.Test;

import static com.nurkiewicz.rxjava.R30_Zip.LOREM_IPSUM;

@Ignore
public class R31_WindowBuffer {

	@Test
	public void test_22() throws Exception {
		Flowable
				.range(1,10)
//				.buffer(3)
//                .buffer(3,1)
                .buffer(3,7)
				.subscribe(System.out::println);
	}
	
	/**
	 * Hint: use buffer()
	 */
	@Test
	public void everyThirdWordUsingBuffer() throws Exception {
		//given
		Flowable<String> everyThirdWord = LOREM_IPSUM;

        LOREM_IPSUM
                .buffer(3)
                .map(i ->i.get(i.size() -1))
                .subscribe(System.out::println);

		
		//then
//		everyThirdWord
//				.test()
//				.assertValues("dolor", "consectetur")
//				.assertNoErrors();
	}
	
	/**
	 * Hint: use window()
	 * Hint: use elementAt()
	 */
	@Test
	public void everyThirdWordUsingWindow() throws Exception {
		//given
		Flowable<String> everyThirdWord = LOREM_IPSUM;
		
		//then
		everyThirdWord
				.test()
				.assertValues("dolor", "consectetur")
				.assertNoErrors();
	}
	
}
