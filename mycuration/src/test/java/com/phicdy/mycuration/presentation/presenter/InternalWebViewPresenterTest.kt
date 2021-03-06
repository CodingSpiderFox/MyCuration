package com.phicdy.mycuration.presentation.presenter

import android.view.KeyEvent
import com.phicdy.mycuration.presentation.view.InternalWebViewView
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times


class InternalWebViewPresenterTest {

    private lateinit var mockView: InternalWebViewView
    @Before
    fun setup() {
        mockView = Mockito.mock(InternalWebViewView::class.java)
    }

    @Test
    fun `initWebView is called when onCreate with valid url`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.create()
        Mockito.verify(mockView, times(1)).initWebView()
    }

    @Test
    fun `initWebView is not called when onCreate with invalid url`() {
        val presenter = InternalWebViewPresenter(mockView, "gjrhaogiarohjgpai")
        presenter.create()
        Mockito.verify(mockView, times(0)).initWebView()
    }

    @Test
    fun `url is loaded when onCreate with valid url`() {
        val url = "http://www.google.com"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.create()
        Mockito.verify(mockView, times(1)).load(url)
    }

    @Test
    fun `url is not loaded when onCreate with invalid url`() {
        val url = "gjrhiqgha@"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.create()
        Mockito.verify(mockView, times(0)).load(url)
    }

    @Test
    fun `url is shared when valid url`() {
        val url = "http://www.google.com"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.onShareMenuClicked()
        Mockito.verify(mockView, times(1)).share(url)
    }

    @Test
    fun `url is not shared when invalid url`() {
        val url = "gjrahighai@j"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.onShareMenuClicked()
        Mockito.verify(mockView, times(0)).share(url)
    }

    @Test
    fun `go back when can go back and back button is clicked`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.onKeyDown(KeyEvent.KEYCODE_BACK, Mockito.mock(KeyEvent::class.java), true)
        Mockito.verify(mockView, times(1)).goBack()
    }

    @Test
    fun `not handle key event when not can go back and back button is clicked`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        val mockEvent = Mockito.mock(KeyEvent::class.java)
        presenter.onKeyDown(KeyEvent.KEYCODE_BACK, mockEvent, false)
        Mockito.verify(mockView, times(1)).parentOnKeyDown(KeyEvent.KEYCODE_BACK, mockEvent)
    }

    @Test
    fun `not handle key event when can go back but back button is not clicked`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        val mockEvent = Mockito.mock(KeyEvent::class.java)
        presenter.onKeyDown(KeyEvent.KEYCODE_HOME, mockEvent, true)
        Mockito.verify(mockView, times(1)).parentOnKeyDown(KeyEvent.KEYCODE_HOME, mockEvent)
    }

    @Test
    fun testOnResume() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.resume()
    }

    @Test
    fun testOnPause() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.pause()
    }

    @Test
    fun `Mode changes to PC when PC mode menu is clicked()`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.onPcModeMenuClicked()
        Mockito.verify(mockView, times(1)).setPcMode()
    }

    @Test
    fun `URL is reloaded when PC mode menu is clicked()`() {
        val url = "http://www.google.com"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.onPcModeMenuClicked()
        Mockito.verify(mockView, times(1)).load(url)
    }

    @Test
    fun `Mode changes to Mobile when Mobile mode menu is clicked()`() {
        val presenter = InternalWebViewPresenter(mockView, "http://www.google.com")
        presenter.onMobileModeMenuClicked()
        Mockito.verify(mockView, times(1)).setMobileMode()
    }

    @Test
    fun `URL is reloaded when Mobile mode menu is clicked()`() {
        val url = "http://www.google.com"
        val presenter = InternalWebViewPresenter(mockView, url)
        presenter.onMobileModeMenuClicked()
        Mockito.verify(mockView, times(1)).load(url)
    }
}