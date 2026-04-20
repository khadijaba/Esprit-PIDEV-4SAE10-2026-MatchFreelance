import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProjectService } from './project.service';
import { Project, ProjectRequest, ProjectStatus } from '../models/project.model';

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/projects';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProjectService]
    });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('should fetch all projects', () => {
      const mockProjects: Project[] = [
        { id: 1, title: 'Project 1', description: 'Description 1', status: 'OPEN' } as Project,
        { id: 2, title: 'Project 2', description: 'Description 2', status: 'IN_PROGRESS' } as Project
      ];

      service.getAll().subscribe(projects => {
        expect(projects).toEqual(mockProjects);
        expect(projects.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('getById', () => {
    it('should fetch project by id', () => {
      const mockProject: Project = {
        id: 1,
        title: 'Test Project',
        description: 'Test Description',
        status: 'OPEN',
        budget: 5000,
        clientId: 100
      } as Project;

      service.getById(1).subscribe(project => {
        expect(project).toEqual(mockProject);
        expect(project.id).toBe(1);
        expect(project.title).toBe('Test Project');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProject);
    });
  });

  describe('getByStatus', () => {
    it('should fetch projects by status', () => {
      const mockProjects: Project[] = [
        { id: 1, title: 'Project 1', status: 'OPEN' } as Project,
        { id: 2, title: 'Project 2', status: 'OPEN' } as Project
      ];

      service.getByStatus('OPEN').subscribe(projects => {
        expect(projects).toEqual(mockProjects);
        expect(projects.length).toBe(2);
        projects.forEach(p => expect(p.status).toBe('OPEN'));
      });

      const req = httpMock.expectOne(`${apiUrl}/status/OPEN`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('search', () => {
    it('should search projects by title', () => {
      const mockProjects: Project[] = [
        { id: 1, title: 'Web Development', status: 'OPEN' } as Project
      ];

      service.search('Web').subscribe(projects => {
        expect(projects).toEqual(mockProjects);
        expect(projects[0].title).toContain('Web');
      });

      const req = httpMock.expectOne(req => req.url === `${apiUrl}/search`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('title')).toBe('Web');
      req.flush(mockProjects);
    });
  });

  describe('getByClientId', () => {
    it('should fetch projects by client id', () => {
      const mockProjects: Project[] = [
        { id: 1, title: 'Project 1', clientId: 100, status: 'OPEN' } as Project,
        { id: 2, title: 'Project 2', clientId: 100, status: 'IN_PROGRESS' } as Project
      ];

      service.getByClientId(100).subscribe(projects => {
        expect(projects).toEqual(mockProjects);
        expect(projects.length).toBe(2);
        projects.forEach(p => expect(p.clientId).toBe(100));
      });

      const req = httpMock.expectOne(`${apiUrl}/client/100`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('create', () => {
    it('should create a new project', () => {
      const projectRequest: ProjectRequest = {
        title: 'New Project',
        description: 'New Description',
        budget: 3000,
        status: 'OPEN',
        clientId: 100
      };

      const mockResponse: Project = {
        id: 1,
        ...projectRequest
      } as Project;

      service.create(projectRequest).subscribe(project => {
        expect(project).toEqual(mockResponse);
        expect(project.id).toBe(1);
        expect(project.title).toBe('New Project');
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(projectRequest);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update an existing project', () => {
      const projectRequest: ProjectRequest = {
        title: 'Updated Project',
        description: 'Updated Description',
        budget: 4000,
        status: 'IN_PROGRESS',
        clientId: 100
      };

      const mockResponse: Project = {
        id: 1,
        ...projectRequest
      } as Project;

      service.update(1, projectRequest).subscribe(project => {
        expect(project).toEqual(mockResponse);
        expect(project.title).toBe('Updated Project');
        expect(project.budget).toBe(4000);
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(projectRequest);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a project', () => {
      service.delete(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('error handling', () => {
    it('should handle 404 error when project not found', () => {
      service.getById(999).subscribe(
        () => fail('should have failed with 404 error'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 500 error on server failure', () => {
      service.getAll().subscribe(
        () => fail('should have failed with 500 error'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne(apiUrl);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
