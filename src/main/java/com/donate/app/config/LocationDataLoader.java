package com.donate.app.config;

import com.donate.app.model.Localidad;
import com.donate.app.model.Provincia;
import com.donate.app.repository.LocalidadRepository;
import com.donate.app.repository.ProvinciaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class LocationDataLoader implements ApplicationRunner {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;

    public LocationDataLoader(
            ProvinciaRepository provinciaRepository,
            LocalidadRepository localidadRepository
    ) {
        this.provinciaRepository = provinciaRepository;
        this.localidadRepository = localidadRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (provinciaRepository.count() > 0) {
            return;
        }

        Map<String, List<String>> seed = Map.of(
                "Buenos Aires", List.of("La Plata", "Mar del Plata", "Bahía Blanca", "Quilmes", "Lanús"),
                "Córdoba", List.of("Córdoba Capital", "Villa María", "Río Cuarto", "Alta Gracia"),
                "Santa Fe", List.of("Rosario", "Santa Fe Capital", "Rafaela", "Venado Tuerto"),
                "Mendoza", List.of("Mendoza Capital", "San Rafael", "Godoy Cruz"),
                "Tucumán", List.of("San Miguel de Tucumán", "Yerba Buena", "Tafí Viejo")
        );

        seed.forEach(this::createProvinciaWithLocalidades);
    }

    private void createProvinciaWithLocalidades(String provinciaNombre, List<String> localidades) {
        Provincia provincia = new Provincia();
        provincia.setNombre(provinciaNombre);
        provincia = provinciaRepository.save(provincia);

        for (String localidadNombre : localidades) {
            Localidad localidad = new Localidad();
            localidad.setNombre(localidadNombre);
            localidad.setProvincia(provincia);
            localidadRepository.save(localidad);
        }
    }
}
