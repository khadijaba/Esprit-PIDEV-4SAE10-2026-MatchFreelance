import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ContractService } from './contract.service';
import { Contract, ContractRequest } from '../models/contract.model';
import { FinancialSummary, ContractHealth } from '../models/contract-advanced.model';

describe('ContractService', () => {
  let service: ContractService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/contracts';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ContractService]
    });
    service = TestBed.inject(ContractService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('should fetch all contracts', () => {
      const mockContracts: Contract[] = [
        { id: 1, projectId: 100, freelancerId: 200, clientId: 300, status: 'ACTIVE' } as Contract,
        { id: 2, projectId: 101, freelancerId: 201, clientId: 301, status: 'DRAFT' } as Contract
      ];

      service.getAll().subscribe(contracts => {
        expect(contracts).toEqual(mockContracts);
        expect(contracts.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockContracts);
    });
  });

  describe('getById', () => {
    it('should fetch contract by id', () => {
      const mockContract: Contract = {
        id: 1,
        projectId: 100,
        freelancerId: 200,
        clientId: 300,
        status: 'ACTIVE',
        proposedBudget: 1000,
        terms: 'Test terms'
      } as Contract;

      service.getById(1).subscribe(contract => {
        expect(contract).toEqual(mockContract);
        expect(contract.id).toBe(1);
        expect(contract.status).toBe('ACTIVE');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContract);
    });
  });

  describe('getByProjectId', () => {
    it('should fetch contracts by project id', () => {
      const mockContracts: Contract[] = [
        { id: 1, projectId: 100, freelancerId: 200, clientId: 300, status: 'ACTIVE' } as Contract
      ];

      service.getByProjectId(100).subscribe(contracts => {
        expect(contracts).toEqual(mockContracts);
        expect(contracts[0].projectId).toBe(100);
      });

      const req = httpMock.expectOne(`${apiUrl}/project/100`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContracts);
    });
  });

  describe('getByFreelancerId', () => {
    it('should fetch contracts by freelancer id', () => {
      const mockContracts: Contract[] = [
        { id: 1, projectId: 100, freelancerId: 200, clientId: 300, status: 'ACTIVE' } as Contract
      ];

      service.getByFreelancerId(200).subscribe(contracts => {
        expect(contracts).toEqual(mockContracts);
        expect(contracts[0].freelancerId).toBe(200);
      });

      const req = httpMock.expectOne(`${apiUrl}/freelancer/200`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContracts);
    });
  });

  describe('getByClientId', () => {
    it('should fetch contracts by client id', () => {
      const mockContracts: Contract[] = [
        { id: 1, projectId: 100, freelancerId: 200, clientId: 300, status: 'ACTIVE' } as Contract
      ];

      service.getByClientId(300).subscribe(contracts => {
        expect(contracts).toEqual(mockContracts);
        expect(contracts[0].clientId).toBe(300);
      });

      const req = httpMock.expectOne(`${apiUrl}/client/300`);
      expect(req.request.method).toBe('GET');
      req.flush(mockContracts);
    });
  });

  describe('create', () => {
    it('should create a new contract', () => {
      const contractRequest: ContractRequest = {
        projectId: 100,
        freelancerId: 200,
        clientId: 300,
        terms: 'New contract terms',
        proposedBudget: 2000,
        status: 'DRAFT'
      };

      const mockResponse: Contract = {
        id: 1,
        ...contractRequest
      } as Contract;

      service.create(contractRequest).subscribe(contract => {
        expect(contract).toEqual(mockResponse);
        expect(contract.id).toBe(1);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(contractRequest);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update an existing contract', () => {
      const contractRequest: ContractRequest = {
        projectId: 100,
        freelancerId: 200,
        clientId: 300,
        terms: 'Updated terms',
        proposedBudget: 2500,
        status: 'ACTIVE'
      };

      const mockResponse: Contract = {
        id: 1,
        ...contractRequest
      } as Contract;

      service.update(1, contractRequest).subscribe(contract => {
        expect(contract).toEqual(mockResponse);
        expect(contract.proposedBudget).toBe(2500);
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(contractRequest);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a contract', () => {
      service.delete(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('proposeExtraBudget', () => {
    it('should propose extra budget', () => {
      const mockResponse: Contract = {
        id: 1,
        pendingExtraAmount: 500,
        pendingExtraReason: 'Additional work'
      } as Contract;

      service.proposeExtraBudget(1, 500, 'Additional work', 200).subscribe(contract => {
        expect(contract).toEqual(mockResponse);
        expect(contract.pendingExtraAmount).toBe(500);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/propose-extra-budget`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({
        amount: 500,
        reason: 'Additional work',
        freelancerId: 200
      });
      req.flush(mockResponse);
    });
  });

  describe('updateProgress', () => {
    it('should update contract progress', () => {
      const mockResponse: Contract = {
        id: 1,
        progressPercent: 75
      } as Contract;

      service.updateProgress(1, 75, 200).subscribe(contract => {
        expect(contract).toEqual(mockResponse);
        expect(contract.progressPercent).toBe(75);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/progress`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({
        progressPercent: 75,
        freelancerId: 200
      });
      req.flush(mockResponse);
    });
  });

  describe('getFinancialSummary', () => {
    it('should fetch financial summary', () => {
      const mockSummary: FinancialSummary = {
        contractId: 1,
        baseBudget: 1000,
        extraTasksBudget: 200,
        totalContractValue: 1200,
        platformFeePercent: 10,
        platformFeeAmount: 120,
        freelancerNetAmount: 1080,
        clientTotalAmount: 1200,
        amountReleasableByProgress: 600,
        paymentSchedule: []
      };

      service.getFinancialSummary(1).subscribe(summary => {
        expect(summary).toEqual(mockSummary);
        expect(summary.totalContractValue).toBe(1200);
        expect(summary.platformFeeAmount).toBe(120);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/financial-summary`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSummary);
    });
  });

  describe('getContractHealth', () => {
    it('should fetch contract health', () => {
      const mockHealth: ContractHealth = {
        contractId: 1,
        overallScore: 85,
        communicationScore: 90,
        progressScore: 80,
        issuesDetected: []
      } as ContractHealth;

      service.getContractHealth(1).subscribe(health => {
        expect(health).toEqual(mockHealth);
        expect(health.overallScore).toBe(85);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/health`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHealth);
    });
  });

  describe('downloadPdf', () => {
    it('should download PDF without signature', () => {
      const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });

      service.downloadPdf(1).subscribe(blob => {
        expect(blob).toEqual(mockBlob);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/pdf`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });

    it('should download PDF with signature', () => {
      const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });
      const signature = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA';

      service.downloadPdf(1, signature).subscribe(blob => {
        expect(blob).toEqual(mockBlob);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/pdf`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ signature });
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });
  });

  describe('rateContract', () => {
    it('should rate a contract', () => {
      const mockResponse: Contract = {
        id: 1,
        clientRating: 5,
        clientReview: 'Excellent work'
      } as Contract;

      service.rateContract(1, 5, 'Excellent work', 300).subscribe(contract => {
        expect(contract).toEqual(mockResponse);
        expect(contract.clientRating).toBe(5);
      });

      const req = httpMock.expectOne(`${apiUrl}/1/rating`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({
        rating: 5,
        review: 'Excellent work',
        clientId: 300
      });
      req.flush(mockResponse);
    });
  });
});
