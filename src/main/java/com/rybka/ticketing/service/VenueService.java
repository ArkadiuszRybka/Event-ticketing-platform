package com.rybka.ticketing.service;

import com.rybka.ticketing.mapper.VenueMapper;
import com.rybka.ticketing.model.dto.admin.VenueCreateDto;
import com.rybka.ticketing.model.dto.admin.VenueReadDto;
import com.rybka.ticketing.model.dto.admin.VenueUpdateDto;
import com.rybka.ticketing.model.entity.Venue;
import com.rybka.ticketing.repository.EventRepository;
import com.rybka.ticketing.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VenueService {
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;

    public VenueReadDto create(VenueCreateDto dto){
        validateGeometry(dto.rows(), dto.seatsPerRow());
        if(venueRepository.existsByName(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists");
        }

        if(dto.rows() * dto.seatsPerRow() > 20000){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Venue too big");

        }

        Venue v = new Venue();
        v.setName(dto.name());
        v.setRows(dto.rows());
        v.setSeatsPerRow(dto.seatsPerRow());
        try {
            v = venueRepository.save(v);
        }catch (DataIntegrityViolationException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists");
        }
        return VenueMapper.toReadDto(v);
    }

    public VenueReadDto update(Long id, VenueUpdateDto dto){
        Venue v = venueRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        if(dto.name() != null){
            String newName = dto.name();
            if(!newName.equalsIgnoreCase(v.getName()) && venueRepository.existsByName(newName)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists");
            }
            v.setName(newName);
        }

        boolean hasEvents = eventRepository.existsByVenue_Id(v.getId()) && eventRepository != null;
        if(dto.row() != null && dto.seatsPerRow() != null){
            if(hasEvents){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue in use by event");
            }
            int newRows = dto.row() != null ? dto.row() : v.getRows();
            int newSeatsPerRow = dto.seatsPerRow() != null ? dto.seatsPerRow() : v.getSeatsPerRow();
            validateGeometry(newRows, newSeatsPerRow);
            if(newRows*newSeatsPerRow > 20000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Venue too big");
            }
            v.setRows(newRows);
            v.setSeatsPerRow(newSeatsPerRow);
        }
        v = venueRepository.save(v);
        return VenueMapper.toReadDto(v);
    }

    public VenueReadDto getById(Long id){
        Venue v = venueRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        return VenueMapper.toReadDto(v);
    }

    public Page<VenueReadDto> getAll(Pageable pageable){
        return venueRepository.findAll(pageable).map(VenueMapper::toReadDto);
    }

    public void delete(Long id){
        venueRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        if(eventRepository.existsByVenue_Id(id) && eventRepository != null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue in use by event");
        }
        venueRepository.deleteById(id);
    }

    public void validateGeometry(int rows, int seatsPerRow){
        if(rows < 1 || seatsPerRow < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid geometry");
        }
        if(rows > 200 || seatsPerRow > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geometry out of range");
        }
    }
}
