package com.fileprocessing.service.concurrency;

import com.fileprocessing.concurrency.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WorkflowExecutorService {

    private final ThreadPoolManager threadPoolManager;

}
