package com.counter.maintainer.repository;

import com.counter.maintainer.data.contracts.CounterDetails;
import com.counter.maintainer.data.contracts.CounterType;
import com.counter.maintainer.data.contracts.ServiceType;
import com.counter.maintainer.data.contracts.TokenType;
import com.counter.maintainer.exceptions.BranchNotExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CounterRepositoryImpl implements CounterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;



    @Transactional(readOnly=true)
    public List<ServiceType> findCounterServices(long counterId) {
        return jdbcTemplate.query("select serviceName from Service where serviceId in "
                                      + "(select serviceID from CounterService where counterId=?)", new Object[]{counterId}, new ServiceTypeRowMapper());
    }

    @Transactional(readOnly=true)
    public List<Long> findCounterTokens(long counterId) {
        return jdbcTemplate.queryForList("select DISTINCT tokenId from TokenStatus where counterId=? and inQ='true'", new Object[]{counterId}, Long.class);
    }

    @Transactional(readOnly=true)
    public List<CounterDetails> getAvailableCounters() {

        List<CounterDetails> counterIdList = jdbcTemplate.query("select c.counterId, c.employeeId, c.active, c.counterType, b.branchName from Counter c left join branch b on c.branchId=b.branchId where c.active='true'", new CounterRowMapper());
        return getCounterDetails(counterIdList);
    }

    @Transactional(readOnly=true)
    public List<CounterDetails> getAvailableCounters(String branchName) {

        List<CounterDetails> counterIdList = jdbcTemplate.query("select c.counterId, c.employeeId, c.active, c.counterType, b.branchName "
                                                                    + "from Counter c left join branch b on c.branchId=b.branchId where UPPER(b.branchName) like UPPER(?1) and c.active='true'", new Object[]{branchName}, new CounterRowMapper());
        return getCounterDetails(counterIdList);
    }

    private List<CounterDetails> getCounterDetails(List<CounterDetails> counterIdList) {
        List<CounterDetails> counterDetailsList = new ArrayList<CounterDetails>();
        for(CounterDetails counterDetails: counterIdList) {
            counterDetails.setTokenIdList(findCounterTokens(counterDetails.getCounterId()));
            counterDetails.setServiceTypes(findCounterServices(counterDetails.getCounterId()));
            counterDetails.setActive(true);
            counterDetailsList.add(counterDetails);
        }

        return counterDetailsList;

    }
}

class  CounterRowMapper implements RowMapper<CounterDetails>
{
    @Override
    public CounterDetails mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        CounterDetails counterDetails = new CounterDetails();
        counterDetails.setCounterId(rs.getInt("counterId"));
        counterDetails.setActive(rs.getBoolean("active"));
        counterDetails.setEmployeeId(rs.getLong("employeeId"));
        counterDetails.setCounterType(CounterType.valueOf(rs.getString("counterType")));
        counterDetails.setBranchName(rs.getString("branchName"));
        return counterDetails;
    }
}

class ServiceTypeRowMapper implements RowMapper<ServiceType>
{
    @Override
    public ServiceType mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        String serviceName = rs.getString("serviceName");

        ServiceType serviceType = ServiceType.valueOf(serviceName);
        return serviceType;
    }
}
