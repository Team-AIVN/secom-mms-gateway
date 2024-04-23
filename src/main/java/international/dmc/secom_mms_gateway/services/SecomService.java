package international.dmc.secom_mms_gateway.services;

import lombok.extern.slf4j.Slf4j;
import org.grad.secom.springboot3.components.SecomClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class SecomService {

    private SecomClient secomClient;

    @PostConstruct
    public void init() {
    }
}
