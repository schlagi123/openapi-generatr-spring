/*
 * Copyright 2019 the original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.hauner.openapi.generatr

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.hauner.openapi.spring.generatr.SpringGeneratr
import groovy.io.FileType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


import static org.junit.Assert.assertEquals

abstract class GeneratrTestBase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    TestSet testSet

    GeneratrTestBase(TestSet testSet) {
        this.testSet = testSet
    }

    @Test
    void "generatr creates expected files for api set "() {
        def source = testSet.name
        def packageName = 'generated'
        def expectedPath = ['.', 'src', 'testInt', 'resources', source, packageName].join(File.separator)
        def generatedPath = [folder.root.absolutePath, packageName].join(File.separator)

        def generatr = new SpringGeneratr()
        def options = [
            apiPath: "./src/testInt/resources/${source}/openapi.yaml",
            targetDir: folder.root,
            packageName: packageName,
            beanValidation: testSet.beanValidation
        ]

        def mapping = new File("./src/testInt/resources/${source}/mapping.yaml")
        if(mapping.exists ()) {
            options.typeMappings = mapping
        }

        when:
        generatr.run (options)

        then:
        def generatedFiles = collectGenerated(generatedPath)
        def expectedFiles = collectExpected(expectedPath)
        assert generatedFiles == expectedFiles

        expectedFiles.each {
            def expected = new File([expectedPath, it].join ('/'))
            def generated = new File([generatedPath, it].join ('/'))

            printUnifiedDiff (expected, generated)
            assertEquals(
                // ignore cr (ie. crlf vs lf)
                expected.text.replace('\r',''),
                generated.text.replace('\r','')
            )
        }
    }

    void printUnifiedDiff (File expected, File generated) {
        def patch = DiffUtils.diff (
            expected.readLines (),
            generated.readLines ())

        def diff = UnifiedDiffUtils.generateUnifiedDiff (
            expected.path,
            generated.path,
            expected.readLines (),
            patch,
            2
        )

        diff.each {
            println it
        }
    }

    List<String> collectGenerated(String generatedPath) {
        def generated = []
        new File(generatedPath).eachFileRecurse FileType.FILES,  {
            generated << it.absolutePath.replace (generatedPath, '')
        }
        generated
    }

    List<String> collectExpected(String expectedPath) {
        def expected = []
        new File(expectedPath).eachFileRecurse FileType.FILES,  {
            expected << it.path.replace (expectedPath, '')
        }
        expected
    }

}
