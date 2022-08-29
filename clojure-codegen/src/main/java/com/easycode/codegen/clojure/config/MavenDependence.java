package com.easycode.codegen.clojure.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @class-name: MavenDependence
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-29 15:33
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MavenDependence {

    private String groupId;

    private String artifactId;

    private String version;

}
