package impl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriverService;

import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CityOfOttawaBooking {
    WebDriver driver;
    String bookingUrl;
    String date = "//*[@aria-label='7:30 PM Saturday June 8, 2024']";
    String type = "div.content:contains(Badminton)";

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver.exe");
        long initialDelay = getInitialDelayToNextWednesday6PM();
        CityOfOttawaBooking booking = new CityOfOttawaBooking();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        ChromeDriverService service = new ChromeDriverService.Builder().build();
        WebDriver driver = new ChromeDriver(service, options);
        booking.setDriver(driver);
        try {
            booking.openBrowserAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                booking.checkAvailability();
            }
        };
        scheduler.schedule(task, initialDelay, TimeUnit.NANOSECONDS);
    }

    private static long getInitialDelayToNextWednesday6PM() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime nextWednesday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)).withHour(18).withMinute(0).withSecond(0).withNano(100);

        if (now.isAfter(nextWednesday)) {
            nextWednesday = nextWednesday.plusWeeks(1);
        }

        Duration duration = Duration.between(now, nextWednesday);
        return duration.toNanos();
    }

    public void openBrowserAndWait() throws IOException {
        Document doc = Jsoup.connect("https://reservation.frontdesksuite.ca/rcfs/nepeansportsplex/Home/Index?Culture=en&PageId=b0d362a1-ba36-42ae-b1e0-feefaf43fe4c&ShouldStartReserveTimeFlow=False&ButtonId=00000000-0000-0000-0000-000000000000").get();
        Element targetDiv = doc.selectFirst(type); // Change activity name as needed

        if (targetDiv == null) {
            return;
        }
        driver.get("https://reservation.frontdesksuite.ca/rcfs/nepeansportsplex/Home/Index?Culture=en&PageId=b0d362a1-ba36-42ae-b1e0-feefaf43fe4c&ShouldStartReserveTimeFlow=False&ButtonId=00000000-0000-0000-0000-000000000000");
        bookingUrl = "https://reservation.frontdesksuite.ca/" + targetDiv.parent().attr("href");
    }


    public void checkAvailability() {
        try {
            driver.get(bookingUrl);
            try {
                WebElement numOfPplInputField = driver.findElement(By.id("reservationCount"));
                String typeAttributeValue = numOfPplInputField.getAttribute("type");
                if (typeAttributeValue != null && typeAttributeValue.equalsIgnoreCase("hidden")) {
                    return;
                }
                numOfPplInputField.clear();
                numOfPplInputField.sendKeys("1");
                driver.findElement(By.id("submit-btn")).click();
            } catch (NoSuchElementException e) {
                return;
            }

            WebElement dateList = driver.findElement(By.className("date-list"));
            if (dateList == null) {
                return;
            }

            List<WebElement> childElements = dateList.findElements(By.xpath("*"));
            if (!childElements.isEmpty()) {
                WebElement lastChild = childElements.get(childElements.size() - 1);
                WebElement expandBtn = lastChild.findElement(By.className("title-padded"));
                expandBtn.click();

                WebElement timeBtn = lastChild.findElement(By.xpath(date)); // Modify timeslot as needed
                timeBtn.click();

                driver.findElement(By.id("telephone")).sendKeys("3437771827"); // Modify phone number as needed
                driver.findElement(By.id("email")).sendKeys("yuchengguang2014@gmail.com"); // Modify email as needed
                driver.findElements(new By.ByCssSelector("input[type='text']")).getFirst().sendKeys("Chengguang Yu");

                Thread.sleep(150);
                driver.findElement(By.id("submit-btn")).click();
            }
            synchronized (CityOfOttawaBooking.class) {
                CityOfOttawaBooking.class.wait();
            }
            driver.quit();

        } catch ( InterruptedException e) {
            e.printStackTrace();
        }
    }
}