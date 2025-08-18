#!/usr/bin/env python3
"""
BigDecimal Migration Deployment Readiness Assessment Tool

This script performs a comprehensive assessment of the system's readiness
for BigDecimal migration deployment by checking various criteria and
generating a detailed readiness report.
"""

import json
import subprocess
import sys
import os
import requests
import time
from datetime import datetime
from typing import Dict, List, Tuple, Any
import mysql.connector
from mysql.connector import Error

class DeploymentReadinessAssessment:
    def __init__(self, config_file: str = None):
        self.config = self.load_config(config_file)
        self.results = {
            'timestamp': datetime.now().isoformat(),
            'overall_status': 'UNKNOWN',
            'readiness_score': 0,
            'categories': {},
            'critical_issues': [],
            'warnings': [],
            'recommendations': []
        }
        
    def load_config(self, config_file: str) -> Dict[str, Any]:
        """Load configuration from file or use defaults"""
        default_config = {
            'api_base_url': 'http://localhost:8080',
            'database': {
                'host': 'localhost',
                'database': 'dcf_calculator',
                'user': 'dcf_user',
                'password': os.getenv('DB_PASSWORD', '')
            },
            'thresholds': {
                'response_time_ms': 2000,
                'memory_usage_mb': 2048,
                'test_pass_rate': 95.0,
                'query_time_ms': 1000
            }
        }
        
        if config_file and os.path.exists(config_file):
            with open(config_file, 'r') as f:
                user_config = json.load(f)
                default_config.update(user_config)
                
        return default_config
    
    def assess_code_quality(self) -> Dict[str, Any]:
        """Assess code quality and test coverage"""
        print("Assessing code quality...")
        
        results = {
            'score': 0,
            'max_score': 100,
            'details': {},
            'issues': []
        }
        
        try:
            # Run backend tests
            backend_result = subprocess.run(
                ['mvn', 'test', '-q'],
                cwd='backend',
                capture_output=True,
                text=True,
                timeout=300
            )
            
            if backend_result.returncode == 0:
                results['details']['backend_tests'] = 'PASSED'
                results['score'] += 30
            else:
                results['details']['backend_tests'] = 'FAILED'
                results['issues'].append('Backend tests failing')
                
        except subprocess.TimeoutExpired:
            results['details']['backend_tests'] = 'TIMEOUT'
            results['issues'].append('Backend tests timed out')
        except Exception as e:
            results['details']['backend_tests'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Backend test execution error: {str(e)}')
        
        try:
            # Check test coverage
            coverage_result = subprocess.run(
                ['mvn', 'jacoco:report', '-q'],
                cwd='backend',
                capture_output=True,
                text=True,
                timeout=120
            )
            
            if coverage_result.returncode == 0:
                results['details']['test_coverage'] = 'GENERATED'
                results['score'] += 20
            else:
                results['details']['test_coverage'] = 'FAILED'
                results['issues'].append('Test coverage report generation failed')
                
        except Exception as e:
            results['details']['test_coverage'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Coverage report error: {str(e)}')
        
        try:
            # Run static analysis
            spotbugs_result = subprocess.run(
                ['mvn', 'spotbugs:check', '-q'],
                cwd='backend',
                capture_output=True,
                text=True,
                timeout=180
            )
            
            if spotbugs_result.returncode == 0:
                results['details']['static_analysis'] = 'PASSED'
                results['score'] += 25
            else:
                results['details']['static_analysis'] = 'ISSUES_FOUND'
                results['issues'].append('Static analysis found issues')
                
        except Exception as e:
            results['details']['static_analysis'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Static analysis error: {str(e)}')
        
        try:
            # Check dependency vulnerabilities
            security_result = subprocess.run(
                ['mvn', 'dependency-check:check', '-q'],
                cwd='backend',
                capture_output=True,
                text=True,
                timeout=300
            )
            
            if security_result.returncode == 0:
                results['details']['security_scan'] = 'PASSED'
                results['score'] += 25
            else:
                results['details']['security_scan'] = 'VULNERABILITIES_FOUND'
                results['issues'].append('Security vulnerabilities detected')
                
        except Exception as e:
            results['details']['security_scan'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Security scan error: {str(e)}')
        
        return results
    
    def assess_database_readiness(self) -> Dict[str, Any]:
        """Assess database readiness for BigDecimal migration"""
        print("Assessing database readiness...")
        
        results = {
            'score': 0,
            'max_score': 100,
            'details': {},
            'issues': []
        }
        
        try:
            connection = mysql.connector.connect(**self.config['database'])
            cursor = connection.cursor()
            
            # Check database connectivity
            cursor.execute("SELECT 1")
            results['details']['connectivity'] = 'OK'
            results['score'] += 20
            
            # Check table structure
            cursor.execute("""
                SELECT table_name, column_name, data_type, numeric_precision, numeric_scale
                FROM information_schema.columns 
                WHERE table_schema = %s 
                  AND table_name IN ('dcf_inputs', 'dcf_outputs', 'financial_data')
                  AND data_type IN ('double', 'decimal')
            """, (self.config['database']['database'],))
            
            columns = cursor.fetchall()
            decimal_columns = [col for col in columns if col[2] == 'decimal']
            
            if decimal_columns:
                results['details']['bigdecimal_columns'] = f'{len(decimal_columns)} found'
                results['score'] += 30
            else:
                results['details']['bigdecimal_columns'] = 'NOT_FOUND'
                results['issues'].append('BigDecimal columns not implemented')
            
            # Check data integrity
            cursor.execute("SELECT COUNT(*) FROM dcf_inputs")
            input_count = cursor.fetchone()[0]
            
            cursor.execute("SELECT COUNT(*) FROM dcf_outputs")
            output_count = cursor.fetchone()[0]
            
            if input_count > 0 and output_count > 0:
                results['details']['data_integrity'] = f'Inputs: {input_count}, Outputs: {output_count}'
                results['score'] += 25
            else:
                results['details']['data_integrity'] = 'NO_DATA'
                results['issues'].append('No test data available')
            
            # Test query performance
            start_time = time.time()
            cursor.execute("""
                SELECT COUNT(*) FROM dcf_inputs d
                JOIN dcf_outputs o ON d.id = o.dcf_input_id
                WHERE d.discount_rate > 5.0
                LIMIT 100
            """)
            end_time = time.time()
            
            query_time_ms = (end_time - start_time) * 1000
            
            if query_time_ms < self.config['thresholds']['query_time_ms']:
                results['details']['query_performance'] = f'{query_time_ms:.2f}ms'
                results['score'] += 25
            else:
                results['details']['query_performance'] = f'{query_time_ms:.2f}ms (SLOW)'
                results['issues'].append(f'Query performance below threshold: {query_time_ms:.2f}ms')
            
        except Error as e:
            results['details']['database_error'] = str(e)
            results['issues'].append(f'Database connection error: {str(e)}')
        except Exception as e:
            results['details']['unexpected_error'] = str(e)
            results['issues'].append(f'Unexpected database error: {str(e)}')
        finally:
            if 'connection' in locals() and connection.is_connected():
                cursor.close()
                connection.close()
        
        return results
    
    def assess_api_readiness(self) -> Dict[str, Any]:
        """Assess API readiness for BigDecimal handling"""
        print("Assessing API readiness...")
        
        results = {
            'score': 0,
            'max_score': 100,
            'details': {},
            'issues': []
        }
        
        try:
            # Check application health
            health_response = requests.get(
                f"{self.config['api_base_url']}/actuator/health",
                timeout=10
            )
            
            if health_response.status_code == 200:
                results['details']['health_check'] = 'OK'
                results['score'] += 25
            else:
                results['details']['health_check'] = f'HTTP {health_response.status_code}'
                results['issues'].append(f'Health check failed: {health_response.status_code}')
                
        except requests.exceptions.RequestException as e:
            results['details']['health_check'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Health check error: {str(e)}')
        
        try:
            # Test financial data endpoint
            financial_response = requests.get(
                f"{self.config['api_base_url']}/api/financial-data/AAPL",
                timeout=10
            )
            
            if financial_response.status_code == 200:
                response_text = financial_response.text
                
                # Check for scientific notation
                if 'e' in response_text.lower() or 'E' in response_text:
                    results['details']['serialization'] = 'SCIENTIFIC_NOTATION_DETECTED'
                    results['issues'].append('API responses contain scientific notation')
                else:
                    results['details']['serialization'] = 'OK'
                    results['score'] += 25
                
                # Check for decimal values
                if '"' in response_text and '.' in response_text:
                    results['details']['decimal_format'] = 'OK'
                    results['score'] += 25
                else:
                    results['details']['decimal_format'] = 'NO_DECIMALS'
                    results['issues'].append('API responses missing decimal values')
                    
            else:
                results['details']['financial_endpoint'] = f'HTTP {financial_response.status_code}'
                results['issues'].append(f'Financial data endpoint failed: {financial_response.status_code}')
                
        except requests.exceptions.RequestException as e:
            results['details']['financial_endpoint'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Financial endpoint error: {str(e)}')
        
        try:
            # Test DCF calculation endpoint
            dcf_payload = {
                "ticker": "TEST",
                "discountRate": 8.123456,
                "growthRate": 3.987654,
                "terminalGrowthRate": 2.555555,
                "freeCashFlow": 1000000000,
                "revenue": 5000000000,
                "sharesOutstanding": 1000000000
            }
            
            start_time = time.time()
            dcf_response = requests.post(
                f"{self.config['api_base_url']}/api/dcf/calculate",
                json=dcf_payload,
                timeout=30
            )
            end_time = time.time()
            
            response_time_ms = (end_time - start_time) * 1000
            
            if dcf_response.status_code == 200:
                if response_time_ms < self.config['thresholds']['response_time_ms']:
                    results['details']['dcf_performance'] = f'{response_time_ms:.2f}ms'
                    results['score'] += 25
                else:
                    results['details']['dcf_performance'] = f'{response_time_ms:.2f}ms (SLOW)'
                    results['issues'].append(f'DCF calculation too slow: {response_time_ms:.2f}ms')
            else:
                results['details']['dcf_endpoint'] = f'HTTP {dcf_response.status_code}'
                results['issues'].append(f'DCF calculation failed: {dcf_response.status_code}')
                
        except requests.exceptions.RequestException as e:
            results['details']['dcf_endpoint'] = f'ERROR: {str(e)}'
            results['issues'].append(f'DCF endpoint error: {str(e)}')
        
        return results
    
    def assess_system_resources(self) -> Dict[str, Any]:
        """Assess system resource readiness"""
        print("Assessing system resources...")
        
        results = {
            'score': 0,
            'max_score': 100,
            'details': {},
            'issues': []
        }
        
        try:
            # Check Java process memory usage
            java_processes = subprocess.run(
                ['pgrep', '-f', 'dcf-calculator'],
                capture_output=True,
                text=True
            )
            
            if java_processes.stdout.strip():
                pid = java_processes.stdout.strip().split('\n')[0]
                
                memory_info = subprocess.run(
                    ['ps', '-p', pid, '-o', 'rss='],
                    capture_output=True,
                    text=True
                )
                
                if memory_info.stdout.strip():
                    memory_kb = int(memory_info.stdout.strip())
                    memory_mb = memory_kb // 1024
                    
                    if memory_mb < self.config['thresholds']['memory_usage_mb']:
                        results['details']['memory_usage'] = f'{memory_mb}MB'
                        results['score'] += 40
                    else:
                        results['details']['memory_usage'] = f'{memory_mb}MB (HIGH)'
                        results['issues'].append(f'High memory usage: {memory_mb}MB')
                else:
                    results['details']['memory_usage'] = 'UNKNOWN'
                    results['issues'].append('Could not determine memory usage')
            else:
                results['details']['java_process'] = 'NOT_RUNNING'
                results['issues'].append('Application not running')
                
        except Exception as e:
            results['details']['resource_check'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Resource check error: {str(e)}')
        
        try:
            # Check disk space
            disk_info = subprocess.run(
                ['df', '-h', '.'],
                capture_output=True,
                text=True
            )
            
            if disk_info.returncode == 0:
                lines = disk_info.stdout.strip().split('\n')
                if len(lines) > 1:
                    disk_data = lines[1].split()
                    available = disk_data[3]
                    usage_percent = disk_data[4].rstrip('%')
                    
                    if int(usage_percent) < 90:
                        results['details']['disk_space'] = f'{available} available ({usage_percent}% used)'
                        results['score'] += 30
                    else:
                        results['details']['disk_space'] = f'{available} available ({usage_percent}% used) - LOW'
                        results['issues'].append(f'Low disk space: {usage_percent}% used')
                        
        except Exception as e:
            results['details']['disk_check'] = f'ERROR: {str(e)}'
            results['issues'].append(f'Disk space check error: {str(e)}')
        
        try:
            # Check system load
            load_info = subprocess.run(
                ['uptime'],
                capture_output=True,
                text=True
            )
            
            if load_info.returncode == 0:
                results['details']['system_load'] = load_info.stdout.strip()
                results['score'] += 30
                
        except Exception as e:
            results['details']['load_check'] = f'ERROR: {str(e)}'
        
        return results
    
    def generate_recommendations(self) -> List[str]:
        """Generate deployment recommendations based on assessment results"""
        recommendations = []
        
        total_score = sum(cat['score'] for cat in self.results['categories'].values())
        max_total_score = sum(cat['max_score'] for cat in self.results['categories'].values())
        
        if total_score / max_total_score >= 0.9:
            recommendations.append("✅ System is ready for deployment")
            recommendations.append("Proceed with deployment checklist")
        elif total_score / max_total_score >= 0.7:
            recommendations.append("⚠️ System has minor issues but may proceed with caution")
            recommendations.append("Address warnings before deployment")
            recommendations.append("Ensure rollback procedures are ready")
        else:
            recommendations.append("❌ System is not ready for deployment")
            recommendations.append("Address critical issues before proceeding")
            recommendations.append("Re-run assessment after fixes")
        
        # Specific recommendations based on issues
        all_issues = []
        for category in self.results['categories'].values():
            all_issues.extend(category['issues'])
        
        if any('test' in issue.lower() for issue in all_issues):
            recommendations.append("Fix failing tests before deployment")
        
        if any('performance' in issue.lower() or 'slow' in issue.lower() for issue in all_issues):
            recommendations.append("Optimize performance before deployment")
            recommendations.append("Consider load testing with BigDecimal operations")
        
        if any('memory' in issue.lower() for issue in all_issues):
            recommendations.append("Monitor memory usage closely during deployment")
            recommendations.append("Consider increasing JVM heap size")
        
        if any('database' in issue.lower() for issue in all_issues):
            recommendations.append("Verify database migration scripts")
            recommendations.append("Ensure database backup is current")
        
        return recommendations
    
    def run_assessment(self) -> Dict[str, Any]:
        """Run complete deployment readiness assessment"""
        print("Starting BigDecimal Migration Deployment Readiness Assessment...")
        print(f"Timestamp: {self.results['timestamp']}")
        
        # Run all assessment categories
        self.results['categories']['code_quality'] = self.assess_code_quality()
        self.results['categories']['database'] = self.assess_database_readiness()
        self.results['categories']['api'] = self.assess_api_readiness()
        self.results['categories']['system_resources'] = self.assess_system_resources()
        
        # Calculate overall readiness score
        total_score = sum(cat['score'] for cat in self.results['categories'].values())
        max_total_score = sum(cat['max_score'] for cat in self.results['categories'].values())
        self.results['readiness_score'] = (total_score / max_total_score) * 100 if max_total_score > 0 else 0
        
        # Collect all issues
        for category in self.results['categories'].values():
            for issue in category['issues']:
                if any(keyword in issue.lower() for keyword in ['critical', 'failed', 'error', 'not found']):
                    self.results['critical_issues'].append(issue)
                else:
                    self.results['warnings'].append(issue)
        
        # Determine overall status
        if self.results['readiness_score'] >= 90 and not self.results['critical_issues']:
            self.results['overall_status'] = 'READY'
        elif self.results['readiness_score'] >= 70 and len(self.results['critical_issues']) <= 2:
            self.results['overall_status'] = 'READY_WITH_WARNINGS'
        else:
            self.results['overall_status'] = 'NOT_READY'
        
        # Generate recommendations
        self.results['recommendations'] = self.generate_recommendations()
        
        return self.results
    
    def save_report(self, filename: str = None) -> str:
        """Save assessment report to file"""
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"deployment_readiness_assessment_{timestamp}.json"
        
        with open(filename, 'w') as f:
            json.dump(self.results, f, indent=2)
        
        return filename

def main():
    """Main function to run the assessment"""
    config_file = sys.argv[1] if len(sys.argv) > 1 else None
    
    assessment = DeploymentReadinessAssessment(config_file)
    results = assessment.run_assessment()
    
    # Save detailed report
    report_file = assessment.save_report()
    print(f"\nDetailed report saved to: {report_file}")
    
    # Print summary
    print("\n" + "="*60)
    print("DEPLOYMENT READINESS ASSESSMENT SUMMARY")
    print("="*60)
    print(f"Overall Status: {results['overall_status']}")
    print(f"Readiness Score: {results['readiness_score']:.1f}%")
    
    print(f"\nCategory Scores:")
    for category, data in results['categories'].items():
        score_pct = (data['score'] / data['max_score']) * 100 if data['max_score'] > 0 else 0
        print(f"  {category.replace('_', ' ').title()}: {score_pct:.1f}% ({data['score']}/{data['max_score']})")
    
    if results['critical_issues']:
        print(f"\nCritical Issues ({len(results['critical_issues'])}):")
        for issue in results['critical_issues']:
            print(f"  ❌ {issue}")
    
    if results['warnings']:
        print(f"\nWarnings ({len(results['warnings'])}):")
        for warning in results['warnings'][:5]:  # Show first 5 warnings
            print(f"  ⚠️ {warning}")
        if len(results['warnings']) > 5:
            print(f"  ... and {len(results['warnings']) - 5} more warnings")
    
    print(f"\nRecommendations:")
    for recommendation in results['recommendations']:
        print(f"  {recommendation}")
    
    # Exit with appropriate code
    if results['overall_status'] == 'READY':
        sys.exit(0)
    elif results['overall_status'] == 'READY_WITH_WARNINGS':
        sys.exit(1)
    else:
        sys.exit(2)

if __name__ == "__main__":
    main()