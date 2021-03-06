/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.dtable.client.widget.analysis;

import java.util.ArrayList;
import java.util.Collections;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.drools.workbench.models.datamodel.imports.Import;
import org.drools.workbench.models.datamodel.oracle.DataType;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.client.resources.i18n.AnalysisConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.drools.workbench.screens.guided.dtable.client.widget.analysis.TestUtil.*;
import static org.mockito.Mockito.*;

@RunWith(GwtMockitoTestRunner.class)
public class DecisionTableAnalyzerSubsumptionResolverTest {

    @GwtMock
    AnalysisConstants analysisConstants;

    @GwtMock
    DateTimeFormat dateTimeFormat;

    private AnalyzerProvider analyzerProvider;

    @Before
    public void setUp() throws Exception {
        analyzerProvider = new AnalyzerProvider();

        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "amount" ) ).thenReturn( DataType.TYPE_NUMERIC_INTEGER );
        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "lengthYears" ) ).thenReturn( DataType.TYPE_NUMERIC_INTEGER );
        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "deposit" ) ).thenReturn( DataType.TYPE_NUMERIC_INTEGER );
        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "approved" ) ).thenReturn( DataType.TYPE_BOOLEAN );
        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "insuranceCost" ) ).thenReturn( DataType.TYPE_NUMERIC_INTEGER );
        when( analyzerProvider.getOracle().getFieldType( "LoanApplication", "approvedRate" ) ).thenReturn( DataType.TYPE_NUMERIC_INTEGER );
        when( analyzerProvider.getOracle().getFieldType( "IncomeSource", "type" ) ).thenReturn( DataType.TYPE_STRING );
        when( analyzerProvider.getOracle().getFieldType( "Person", "name" ) ).thenReturn( DataType.TYPE_STRING );

    }

    @Test
    public void testNoIssues() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withConditionIntegerColumn( "application", "LoanApplication", "amount", ">" )
                .withConditionIntegerColumn( "application", "LoanApplication", "amount", "<=" )
                .withConditionIntegerColumn( "application", "LoanApplication", "lengthYears", "==" )
                .withConditionIntegerColumn( "application", "LoanApplication", "deposit", "<" )
                .withStringColumn( "income", "IncomeSource", "type", "==" )
                .withActionSetField( "application", "approved", DataType.TYPE_BOOLEAN )
                .withActionSetField( "application", "insuranceCost", DataType.TYPE_NUMERIC_INTEGER )
                .withActionSetField( "application", "approvedRate", DataType.TYPE_NUMERIC_INTEGER )
                .withData( new Object[][]{
                        {1, "description", 131000, 200000, 30, 20000, "Asset", true, 0, 2},
                        {2, "description", 10000, 100000, 20, 2000, "Job", true, 0, 4},
                        {3, "description", 100001, 130000, 20, 3000, "Job", true, 10, 6},
                        {4, "description", null, null, null, null, null, null, null, null},
                        {5, "description", null, null, null, null, null, null, null, null}} )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertDoesNotContain( "ThisRowIsRedundantTo(1)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(2)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(3)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(4)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(5)", analyzerProvider.getAnalysisReport() );

    }

    @Test
    public void testNoIssues2() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withConditionIntegerColumn( "application", "LoanApplication", "amount", ">" )
                .withConditionIntegerColumn( "application", "LoanApplication", "amount", "<=" )
                .withConditionIntegerColumn( "application", "LoanApplication", "lengthYears", "==" )
                .withConditionIntegerColumn( "application", "LoanApplication", "deposit", "<" )
                .withStringColumn( "income", "IncomeSource", "type", "==" )
                .withActionSetField( "application", "approved", DataType.TYPE_BOOLEAN )
                .withActionSetField( "application", "insuranceCost", DataType.TYPE_NUMERIC_INTEGER )
                .withActionSetField( "application", "approvedRate", DataType.TYPE_NUMERIC_INTEGER )
                .withData( new Object[][]{
                        { 1, "description", 131000, 200000, 30, 20000, "Asset", true, 0, 2 },
                        { 2, "description", 1000, 200000, 30, 20000, "Asset", true, 0, 2 },
                        { 3, "description", 100001, 130000, 20, 3000, "Job", true, 10, 6 } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList()) );

        assertDoesNotContain( "ThisRowIsRedundantTo(1)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(2)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(3)", analyzerProvider.getAnalysisReport() );

    }

    @Test
    @Ignore("This randomly does not pick up the issue. Better that way, I'm hoping future changes will find the cause. Every other test works 100%")
    public void testRedundantRows001() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withNumericColumn( "application", "LoanApplication", "amount", ">" )
                .withNumericColumn( "application", "LoanApplication", "amount", "<=" )
                .withNumericColumn( "application", "LoanApplication", "lengthYears", "==" )
                .withNumericColumn( "application", "LoanApplication", "deposit", "<" )
                .withStringColumn( "income", "IncomeSource", "type", "==" )
                .withActionSetField( "application", "approved", DataType.TYPE_BOOLEAN )
                .withActionSetField( "application", "insuranceCost", DataType.TYPE_NUMERIC )
                .withActionSetField( "application", "approvedRate", DataType.TYPE_NUMERIC )
                .withData( new Object[][]{
                        { 1, "description", 131000, 200000, 30, 20000, "Asset", true, 0, 2 },
                        { 2, "description", 131000, 200000, 30, 20000, "Asset", true, 0, 2 },
                        { 3, "description", 100001, 130000, 20, 3000, "Job", true, 10, 6 } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 1 );
        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 2 );

    }

    @Test
    public void testRedundantRows002() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withStringColumn( "application", "LoanApplication", "amount", ">" )
                .withStringColumn( "person", "Person", "name", "==" )
                .withStringColumn( "income", "IncomeSource", "type", "==" )
                .withActionSetField( "application", "approved", DataType.TYPE_STRING )
                .withData( new Object[][]{
                        { 1, "description", "131000", "Toni", "Asset", "true" },
                        { 2, "description", "131000", "Toni", "Asset", "true" },
                        { 3, "description", "100001", "Michael", "Job", "true" } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 1 );
        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 2 );

    }

    @Test
    public void testRedundantRows003() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withStringColumn( "application", "LoanApplication", "amount", ">" )
                .withStringColumn( "person", "Person", "name", "==" )
                .withEnumColumn( "income", "IncomeSource", "type", "==", "Asset,Job" )
                .withActionSetField( "application", "approved", DataType.TYPE_STRING )
                .withData( new Object[][]{
                        { 1, "description", "131000", "Toni", "Asset", "true" },
                        { 2, "description", "131000", "Toni", "Asset", "true" },
                        { 3, "description", "100001", "Michael", "Job", "true" } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 1 );
        assertContains( "RedundantRows", analyzerProvider.getAnalysisReport(), 2 );

    }

    @Test
    public void testRedundantConditions001() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withEnumColumn( "a", "Person", "name", "==", "Toni,Eder" )
                .withConditionIntegerColumn( "a", "Person", "name", "==" )
                .withData( new Object[][]{ { 1, "description", "Toni", "Toni" } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "RedundantConditions", analyzerProvider.getAnalysisReport() );

    }

    @Test
    public void testRedundantRowsWithConflict() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withConditionIntegerColumn( "a", "Person", "age", ">" )
                .withConditionDoubleColumn( "d", "Account", "deposit", "<" )
                .withActionSetField( "a", "approved", DataType.TYPE_BOOLEAN )
                .withActionSetField( "a", "approved", DataType.TYPE_BOOLEAN )
                .withData( new Object[][]{
                        { 1, "description", 100, 0.0, true, true },
                        { 2, "description", 100, 0.0, true, false } } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertDoesNotContain( "ThisRowIsRedundantTo(1)", analyzerProvider.getAnalysisReport() );
        assertDoesNotContain( "ThisRowIsRedundantTo(2)", analyzerProvider.getAnalysisReport() );

    }

    @Test
    public void testRedundantActionsInOneRow001() throws Exception {
        GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withConditionIntegerColumn( "a", "Person", "name", "==" )
                .withActionSetField( "a", "salary", DataType.TYPE_NUMERIC_INTEGER )
                .withActionSetField( "a", "salary", DataType.TYPE_NUMERIC_INTEGER )
                .withData( new Object[][]{
                        { 1, "description", "Toni", 100, 100 },
                        { 2, "description", "Eder", 200, null },
                        { 3, "description", "Michael", null, 300 },
                        { 4, "description", null, null, null, null, null }
                } )
                .buildTable();

        DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "ValueForFactFieldIsSetTwice(a, salary)", analyzerProvider.getAnalysisReport() );

    }

    @Test
    public void testRedundantActionsInOneRow002() throws Exception {
        final GuidedDecisionTable52 table52 = new ExtendedGuidedDecisionTableBuilder( "org.test",
                                                                                new ArrayList<Import>(),
                                                                                "mytable" )
                .withConditionIntegerColumn( "a", "Person", "name", "==" )
                .withActionInsertFact( "Person", "b", "salary", DataType.TYPE_NUMERIC_INTEGER )
                .withActionSetField( "b", "salary", DataType.TYPE_NUMERIC_INTEGER )
                .withData( new Object[][]{
                        { 1, "description", "Toni", 100, 100 },
                        { 2, "description", "Eder", 200, null },
                        { 3, "description", "Michael", null, 300 },
                        { 4, "description", null, null, null, null, null }
                } )
                .buildTable();

        final DecisionTableAnalyzer analyzer = analyzerProvider.makeAnalyser( table52 );

        analyzer.onValidate( new ValidateEvent( Collections.emptyList() ) );

        assertContains( "ValueForFactFieldIsSetTwice(b, salary)", analyzerProvider.getAnalysisReport() );

    }

}