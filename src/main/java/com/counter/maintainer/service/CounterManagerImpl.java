package com.counter.maintainer.service;

import com.counter.maintainer.data.contracts.*;
import com.counter.maintainer.exceptions.CountersNotAvailableException;
import com.counter.maintainer.repository.CounterRepository;
import com.counter.maintainer.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.counter.maintainer.data.contracts.ServiceType.*;

@Component
public class CounterManagerImpl implements CounterManager {

    private List<CounterDesk> counterList = new ArrayList<>();

    @Autowired
    protected CounterService counterService;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EmployeeRepository employeeRepository;

    ExecutorService executor;

    @EventListener(ApplicationReadyEvent.class)
    public void initCounters() {
        List<CounterDetails> counterDetailsList = counterRepository.getAvailableCounters();

        if(counterDetailsList.isEmpty()) {
            throw new RuntimeException("CounterDetails not available exception");
        }
        executor = Executors.newFixedThreadPool(counterDetailsList.size());
        for(CounterDetails counterDetails: counterDetailsList) {
            List<ServiceType> serviceTypes = getServiceTypeList(counterDetails.getEmployeeId());
            CounterDesk counterDesk = new CounterDesk(counterService, counterDetails, counterDetails.getEmployeeId(), counterDetails.getCounterType(),
                                                      serviceTypes);
            executor.execute(counterDesk);
            counterList.add(counterDesk);
        }
    }

    @Override
    public Token assignTokenToCounter(Token token) {
        return assignToken(token);
    }

    @Override
    public List<CounterDetails> getCounterStatus() {
        return counterService.getCounterStatus();
    }

    private Token assignToken(Token token) {
        List<CounterDesk> counterDesks = getAvailableCounterDesks(token);
        if(counterDesks.isEmpty()) {
            throw new CountersNotAvailableException();
        }
        Integer minQueueLength = Integer.MAX_VALUE;
        CounterDesk minCounterDesk = counterDesks.get(0);
        for(CounterDesk counterDesk : counterDesks) {
            int curMinLength = counterDesk.getMinQueueLength(token.getServicePriority());
            if (minQueueLength > curMinLength) {
                minQueueLength = curMinLength;
                minCounterDesk = counterDesk;
                if (minQueueLength == 0) {
                    //found empty queue, no need to search other queues
                    break;
                }
            }
        }

        addTokenToCounterQueue(minCounterDesk, token);
        tokenService.updateTokenStatus(token.getTokenId(), TokenStatus.QUEUED);
        return token;

    }

    private void addTokenToCounterQueue(CounterDesk counter, Token token) {
        counter.addTokenToQueue(token);
        token.setCounterId(counter.getCounterId());
        tokenService.addTokenToCounter(token.getTokenId(), token.getCounterId());
        token.setStatus(TokenStatus.QUEUED);
    }

    private List<CounterDesk> getAvailableCounterDesks( Token token) {
        if(token.getServicePriority() == ServicePriority.PREMIUM) {
            return counterList.stream().filter(counterDesk ->
                          counterDesk.getCounterType() == CounterType.BOTH
                              || counterDesk.getCounterType() == CounterType.PREMIUM
            ).filter(counterDesk -> {
                         return counterDesk.getServiceTypes().contains(token.peekNextServiceType());
                     }).collect(Collectors.toList());
        } else {
            return counterList.stream().filter(counterDesk ->
                          counterDesk.getCounterType() == CounterType.BOTH
                              || counterDesk.getCounterType() == CounterType.REGULAR
            ).filter(counterDesk -> {
                return counterDesk.getServiceTypes().contains(token.peekNextServiceType());
            }).collect(Collectors.toList());
        }
    }

    public List<ServiceType> getServiceTypeList(Long employeeId) {
        EmployeeRole role = employeeRepository.getEmployeeRole(employeeId);
        switch (role) {
        case MANAGER:
            return Arrays.asList(ACC_VERIFICATION, BALANCE_ENQUIRY, ACC_CLOSE, ACC_OPEN, MANAGER_APPROVAL, DOC_VERIFICATION);
        case OPERATOR:
            return Arrays.asList(ACC_VERIFICATION,BALANCE_ENQUIRY,CASH_WITHDRAW,CASH_DEPOSIT,CHECK_DEPOSIT, DOC_VERIFICATION);
        default:
            throw new RuntimeException("Unknown EmployeeRole :" + role.name());
        }

    }
}
