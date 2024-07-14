package info.kgeorgiy.ja.sitkina.crawler;

import info.kgeorgiy.java.advanced.crawler.AdvancedCrawler;
import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WebCrawler implements AdvancedCrawler {
    private final ExecutorService downloadersServiceExecutor;
    private final ExecutorService extractorsServiceExecutor;
    private final Map<String, HostDownloader> hostDownloaders = new ConcurrentHashMap<>();
    private final Downloader downloader;
    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadersServiceExecutor = Executors.newFixedThreadPool(downloaders);
        extractorsServiceExecutor = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth, Set<String> excludes) {
        DownloadRequestInfo downloadRequestInfo = new DownloadRequestInfo(
                downloader, hostDownloaders, () -> new HostDownloader(perHost), extractorsServiceExecutor
        );
        return downloadRequestInfo.download(Set.of(url), depth,
                (s) -> excludes.stream().noneMatch(s::contains), (s) -> true);
    }

    @Override
    public Result advancedDownload(String url, int depth, List<String> hosts) {
        DownloadRequestInfo downloadRequestInfo = new DownloadRequestInfo(
                downloader, hostDownloaders, () -> new HostDownloader(perHost), extractorsServiceExecutor
        );
        Set<String> hostsSet = new HashSet<>();
        hostsSet.addAll(hosts);
        return downloadRequestInfo.download(Set.of(url), depth, (s) -> true, hostsSet::contains);
    }

    @Override
    public void close() {
        downloadersServiceExecutor.close();
        extractorsServiceExecutor.close();
    }

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Expected non-null args");
            return;
        }
        if (args.length < 2 || !args[0].equals("WebCrawler")) {
            System.err.println("Illegal first/second args");
            return;
        }
        int depth = parseOptionalArgs(args, 2);
        int downloaders = parseOptionalArgs(args, 3);
        int extractors = parseOptionalArgs(args, 4);
        int perHost = parseOptionalArgs(args, 5);
        if (depth < 0 || downloaders < 0 || extractors < 0 || perHost < 0) {
            System.err.println("Illegal integer parameter");
            return;
        }
        CachingDownloader downloader;
        try {
            downloader = new CachingDownloader(0);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
        try (WebCrawler webCrawler = new WebCrawler(downloader, downloaders, extractors, perHost)) {
            Result result = webCrawler.download(args[1], depth);
            System.out.println("Downloaded");
            result.getDownloaded().forEach(System.out::println);
            System.out.println("Errors");
            result.getErrors().forEach((url, e) -> System.out.println(url + " " + e.getMessage()));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static int parseOptionalArgs(String[] args, int idx) {
        if (args.length <= idx) {
            return 1;
        }
        try {
            return Integer.parseInt(args[idx]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static class UrlHost {
        private final String url;
        private final String host;

        public UrlHost(String url, String host) {
            this.url = url;
            this.host = host;
        }

        public void addDownloadTask(Downloader downloader,
                                    Map<String, HostDownloader> hostDownloaders,
                                    Supplier<HostDownloader> createHostDownloader,
                                    DownloadRequestInfo downloadRequestInfo,
                                    Set<String> toAdd,
                                    Boolean addSession,
                                    ExecutorService extractorsService,
                                    CountDownLatch countDownLatch) {
            Runnable task = () -> {
                try {
                    Document res = downloader.download(url);
                    downloadRequestInfo.downloadedDocuments.add(url);
                    addExtractTask(res, downloadRequestInfo, toAdd, extractorsService, countDownLatch);
                } catch (IOException e) {
                    downloadRequestInfo.errors.put(url, e);
                    countDownLatch.countDown();
                }
            };
            var hostDownloader = hostDownloaders.computeIfAbsent(host, (s) -> createHostDownloader.get());
            if (addSession) {
                hostDownloader.sessionCount.getAndIncrement();
            }
//            RunnableFuture<?> future = new FutureTask<>(task, null);
            hostDownloader.submit(task);
        }

        public void addExtractTask(Document document,
                                   DownloadRequestInfo downloadRequestInfo,
                                   Set<String> toAdd,
                                   ExecutorService extractorsService,
                                   CountDownLatch countDownLatch) {
            Runnable task = () -> {
                try {
                    toAdd.addAll(document.extractLinks());
                } catch (IOException e) {
                    downloadRequestInfo.errors.put(url, e);
                }
                countDownLatch.countDown();
            };
            extractorsService.execute(task);
        }
    }

    private static class DownloadRequestInfo {
        public final Map<String, IOException> errors = new ConcurrentHashMap<>();
        public final Set<String> downloadedDocuments = ConcurrentHashMap.newKeySet();
        private final Set<String> usedHosts = new HashSet<>();
        private final Downloader downloader;
        private final Map<String, HostDownloader> hostDownloaders;
        private final Supplier<HostDownloader> createHostDownloader;
        private final ExecutorService extractorsServiceExecutor;

        private DownloadRequestInfo(Downloader downloader, Map<String, HostDownloader> hostDownloaders, Supplier<HostDownloader> createHostDownloader, ExecutorService extractorsServiceExecutor) {
            this.downloader = downloader;
            this.hostDownloaders = hostDownloaders;
            this.createHostDownloader = createHostDownloader;
            this.extractorsServiceExecutor = extractorsServiceExecutor;
        }

        private Result download(Set<String> urlsOnDepth, int depth, Predicate<String> urlFilter, Predicate<String> hostFilter) {
            for (int j = 0; j < depth; j++) {
                Set<String> newSet = ConcurrentHashMap.newKeySet();
                List<UrlHost> dataForRequests = urlsOnDepth.stream()
                        .filter(url -> !downloadedDocuments.contains(url) &&
                                !errors.containsKey(url) && urlFilter.test(url))
                        .map(url -> {
                            try {
                                return new UrlHost(url, URLUtils.getHost(url));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(urlHost -> hostFilter.test(urlHost.host))
                        .toList();
                CountDownLatch countDownLatch = new CountDownLatch(dataForRequests.size());
                dataForRequests.forEach(urlHost -> {
                            urlHost.addDownloadTask(
                                    downloader, hostDownloaders, createHostDownloader,
                                    this, newSet, !usedHosts.contains(urlHost.host),
                                    extractorsServiceExecutor, countDownLatch);
                            usedHosts.add(urlHost.host);
                        }
                );// .toList();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return getResult();
                }
                urlsOnDepth = newSet;
            }
            for (String host : usedHosts) {
                if (hostDownloaders.get(host).decrementAndCheck()) {
                    hostDownloaders.remove(host);
                }
            }
            return getResult();
        }

        private Result getResult() {
            return new Result(downloadedDocuments.stream().toList(), errors);
        }
    }

    private class HostDownloader {
        private final AtomicInteger sessionCount = new AtomicInteger(0);
        private final Queue<Runnable> queue = new ArrayDeque<>();
        private final int perHost;
        private int current = 0;

        private HostDownloader(int perHost) {
            this.perHost = perHost;
        }

        public synchronized void submit(Runnable task) {
            if (current < perHost) {
                current++;
                downloadersServiceExecutor.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        synchronized (HostDownloader.this) {
                            current--;
                            if (!queue.isEmpty()) {
                                submit(queue.remove());
                            }
                        }
                    }
                });
            } else {
                queue.add(task);
            }
        }

        public synchronized boolean decrementAndCheck() {
            return sessionCount.decrementAndGet() == 0 && queue.isEmpty();
        }
    }
}
